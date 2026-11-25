# AI 예문 생성 안정화 플랜

OpenAI 호출(`/api/v2/openai/search`)의 에러 처리, 과금 보호, 대기 시간 상한을 정리한 플랜이다.
결정의 이유와 감수한 점은 [ADR.md](./ADR.md)에 있다.

## 1. 배경: 현재 문제

| 문제 | 위치 | 영향 |
|---|---|---|
| OpenAI 에러 처리 없음 | `ChatClientService.callApi` | 401·429·5xx·타임아웃·JSON 파싱 실패가 모두 500. 클라이언트가 원인을 구분할 수 없음 |
| 사용량 소진이 500 | `SearchController.search` (`IllegalStateException`) | 사용자에게 "한도 소진"을 알려줄 수 없음 |
| 동시 요청 과금 구멍 | `possibleUsage` 확인 → OpenAI 호출 → `decreaseUsage` | 잔량 1에서 동시 N회 요청 시 N회 모두 OpenAI 과금 |
| 타임아웃 없음 + 공격적인 기본 재시도 | `application.yml` (`max-tokens: 300`만 존재) | HTTP 타임아웃이 없다. 여기에 Spring AI 기본 재시도(최대 10회, 2s×5배 백오프, 최대 180s 간격)가 겹쳐 검색 1회가 수십 분 걸리고 OpenAI 호출이 반복될 수 있음 |
| 입력 길이 제한 없음 | `SearchController` 파라미터 | 긴 입력으로 입력 토큰 비용 증가 |
| 부작용 있는 GET | `GET /search` | 단어 저장·사용량 차감이 GET으로 일어남. 재전송·프리페치에 취약 |
| `@Valid` 실패 미처리 | `CustomControllerAdvice` (messaging 패키지 import) | 검증 실패가 400으로 나가지 않음 |

## 2. 범위

**포함**
- 에러 분류와 응답 표준화
- 타임아웃·재시도 정책과 20초 상한 강제
- 선차감 + 실패 시 환불
- 요청당 토큰 상한(입력 길이 검증, `max-tokens`)
- 멱등성 키
- `GET` → `POST` 전환

**제외** (ADR-001, ADR-007 참고)
- 스트리밍 응답
- 비동기 처리와 폴링
- 서비스 전체 비용 상한(OpenAI 대시보드 월 한도, `UsageScheduler` 키 이름 버그)

## 3. 목표 요청 흐름

`POST /api/v2/openai/search`

```
요청 헤더: Authorization: Bearer <access>, Idempotency-Key: <UUID>
요청 바디: { "keyword": "...", "lang": "...", "meaning": "...", "topic": "..." }
```

1. **입력 검증** (`@Valid @RequestBody`)
   - keyword, lang 필수. keyword ≤ 50자, meaning·topic ≤ 100자
   - 실패 → 400 `INVALID_INPUT`
2. **멱등성 키 확인** (선차감보다 먼저)
   - 헤더 없음, UUID 형식 아님 → 400 `INVALID_INPUT` (검증 후 소문자로 정규화)
   - Redis `SET idem:<user>:<key> IN_PROGRESS NX EX 30`
   - Redis 장애 → 503 `TEMPORARILY_UNAVAILABLE` (fail-closed, 차감·호출 없음)
   - 이미 `DONE` → 저장된 응답을 그대로 200으로 반환 (차감·호출 없음)
     - 저장된 응답 복원 실패 → 503 `TEMPORARILY_UNAVAILABLE`. 다시 처리하지 않고 키도 지우지 않는다.
   - 이미 `IN_PROGRESS` → 409 `REQUEST_IN_PROGRESS`
3. **선차감** (기존 조건부 UPDATE `consumeUsage`, 별도 트랜잭션으로 커밋)
   - 잔량 없음 → 멱등성 키 삭제 후 429 `USAGE_EXHAUSTED`
   - 커밋한 뒤에 OpenAI를 호출한다. 호출하는 동안 DB 커넥션을 잡고 있지 않는다.
4. **OpenAI 호출**: 4장의 시간 예산을 따른다.
5. **응답 파싱 → 단어 저장 → 멱등성 키 `DONE` 기록** (응답 JSON 포함, TTL 24h)
6. **실패 처리**: 4 또는 5에서 실패하면
   - 사용량 환불 (+1, 조건부 UPDATE). 환불이 실패해도 원래 예외를 던지고, 환불 예외는 suppressed로 붙인다.
   - 멱등성 키 삭제 (같은 키로 다시 시도할 수 있게). 삭제가 실패하면 WARN 후 TTL(30s) 만료에 맡긴다.
   - 503 `AI_UNAVAILABLE` (단어 저장 실패는 현재 처리하지 않은 예외로 500, 상태 코드 미정)
7. **`DONE` 기록 실패**: WARN 로그 후 200 (ADR-006)

흐름 조립은 `search.SearchService`가 맡는다(`@Transactional` 없음, ADR-005).

### 에러 응답

공통 바디는 `{ "code": "...", "message": "..." }`이다. `CustomControllerAdvice`에서 전용 예외를 매핑한다.

| HTTP | code | 원인 | 사용량 |
|---|---|---|---|
| 400 | `INVALID_INPUT` | 검증 실패, 멱등성 키 누락·형식 오류 | 변화 없음 |
| 409 | `REQUEST_IN_PROGRESS` | 같은 키 요청이 처리 중 | 변화 없음 |
| 429 | `USAGE_EXHAUSTED` | 일일 사용량 소진 | 변화 없음 |
| 503 | `AI_UNAVAILABLE` | OpenAI 5xx·타임아웃·429·400·401·403, 응답 파싱 실패 | 환불 |
| 503 | `TEMPORARILY_UNAVAILABLE` | Redis 장애로 멱등성 키 등록 실패, 저장된 `DONE` 응답 복원 실패 | 변화 없음 |
| 500 | (Spring 기본) | 단어 저장 실패 (상태 코드 미정) | 환불 |

- OpenAI의 400·401·403·429는 재시도하지 않는다. 내부 원인은 사용자에게 숨기고 **ERROR 로그**로 남긴다(키 문제나 크레딧 소진은 운영자가 조치해야 한다).
- 사용자에게 보이는 상태 코드는 503으로 통일한다.

## 4. 타임아웃·재시도와 20초 상한

### 정책
- 재시도 대상: OpenAI **5xx, 연결 타임아웃, 응답 타임아웃**만. 최대 1회(총 2회 시도)
- 재시도 제외: **모든 429**(rate limit과 insufficient_quota를 구분하지 않음), 400, 401, 403, 응답 파싱 실패

### 시간 예산 (최악 경로)

| 구간 | 값 |
|---|---|
| 1차 시도: 연결 + 응답 | 2s + 7s = 9s |
| 백오프 | 1s |
| 2차 시도: 연결 + 응답 | 2s + 7s = 9s |
| **합계** | **19s ≤ 20s** |

> 그릴링 중 합의한 "연결 3s / 응답 10s / 총 20s"는 재시도 1회를 넣으면 최악 26s가 되어 서로 맞지 않는다. 그래서 시도당 값을 위 표처럼 줄였다.

### 상한을 강제하는 방법
**구현 방식: `application.yml` 설정만 사용한다**(코드 추가 없음, ADR-004).

1. **정적 예산**: `(max-attempts + 1) × (connect + read) + backoff ≤ 20s`가 되도록 설정값을 고른다. 이 식은 `application.yml`의 설정 위 주석과 이 문서에 남기고, 값을 바꿀 때 다시 계산한다.
2. **재시도 계층 단일화**: 재시도는 Spring AI 내장 `RetryTemplate`에서만 한다. `ChatClientService`에서는 재시도하지 않는다. 두 계층이 겹치면 시도 횟수가 곱해져 예산이 깨진다.
3. ~~데드라인 확인~~ → **후속 과제**로 미룬다(9장). 재시도 직전에 남은 시간을 확인하려면 `RetryTemplate` 빈을 직접 등록해야 하는데, 정적 예산만으로 최악 19s를 지킬 수 있어서 이번에는 하지 않는다. 그래서 앞 단계(검증, Redis, 선차감)가 비정상적으로 느린 경우에는 20s를 조금 넘을 수 있다.

### 설정 값

```yaml
# common 프로필
spring:
  ai:
    retry:
      max-attempts: 1            # 재시도 1회 = 최대 2번 시도
      on-client-errors: false    # 4xx(429 포함)는 재시도하지 않음. 기본값이지만 의도를 드러내려고 명시
      backoff:
        initial-interval: 1s
  http:
    clients:                     # Boot 4: 복수형 clients (단수형 client는 4.0.0부터 deprecated)
      connect-timeout: 2s
      read-timeout: 7s
```

연결 타임아웃 2s는 서버(미국 중부)에서 `api.openai.com`까지 연결 시간을 실측한 뒤 확정한다.

```
curl -o /dev/null -s -w "connect=%{time_connect}s tls=%{time_appconnect}s\n" https://api.openai.com/v1/models
```

여유가 부족하면 connect 3s + read 6s로 바꿔도 예산은 그대로다(시도당 9s).

### 확인된 사실 (2.0.0-M2 / Boot 4.0.1 jar의 설정 메타데이터와 바이트코드로 확인)
- **Spring AI 재시도 기본값**: `max-attempts=10`, `backoff.initial-interval=2s`, `multiplier=5`, `max-interval=180s`
- **재시도 구현**: Spring Framework 7의 `org.springframework.core.retry.RetryTemplate`. `OpenAiChatModel`이 호출할 때 이를 사용한다.
- **재시도 대상 예외**: `TransientAiException`, `ResourceAccessException`(I/O 오류·타임아웃)
- **HTTP 상태 분류 순서**:
  1. `on-http-codes`에 있는 코드 → 재시도
  2. 4xx이고 `on-client-errors=false` → `NonTransientAiException`(재시도 안 함)
  3. `exclude-on-http-codes`에 있는 코드 → 재시도 안 함
  4. 나머지(5xx) → 재시도

  그래서 **429는 기본 설정에서도 재시도되지 않는다.**
- **빈 교체**: `RetryTemplate`과 `ResponseErrorHandler` 빈은 `@ConditionalOnMissingBean`이다. 직접 등록하면 자동 설정 대신 우리 것이 쓰인다(후속 과제인 데드라인 확인에서 사용).
- **HTTP 타임아웃 설정**: `spring.http.clients.connect-timeout`, `spring.http.clients.read-timeout`. Spring AI 쪽에는 타임아웃 설정이 없다.

### 문서 확인 필요
- `max-attempts`가 `RetryPolicy.maxRetries`에 그대로 들어간다. 그래서 "첫 시도를 뺀 재시도 횟수"로 보이지만, 실제 의미는 Framework 문서로 확인해야 한다. 아래 실측으로도 확인한다.
- Spring AI의 `RestClient`가 `spring.http.clients.*` 타임아웃을 실제로 따르는지. Boot가 고르는 클라이언트 구현에 따라 connect 타임아웃에 TLS 핸드셰이크가 포함되는지도 달라진다. classpath에 Reactor Netty(webflux)가 있다.
- read 타임아웃이 바이트 사이 간격인지, 응답 전체 시간인지. 바이트 사이 간격이라면 응답이 조금씩 계속 오는 경우 7s를 넘을 수 있다.
- `RetryPolicy.Builder.timeout(Duration)`의 의미(후속 과제인 데드라인 확인에 쓸 수 있는지)
- OpenAI 429 에러 본문 형식(나중에 rate limit만 재시도하게 바꿀 경우)

### 적용 확인 방법 (로컬)
1. `read-timeout`을 잠시 `1ms`로 바꾸고 검색을 1회 호출한다.
2. 기대하는 결과:
   - Spring AI 로그 `Retry error. Retry count:1`이 1번 찍힘
   - 약 1s 뒤 503 `AI_UNAVAILABLE` 응답
   - advice의 ERROR 로그에 cause로 `ResourceAccessException`이 남음
3. 원래 값으로 되돌린다.

OpenAI 호출이 나가므로 1~2회 과금된다.

### 멱등성 키 TTL과의 관계
`IN_PROGRESS` TTL(30s)은 상한(20s)에 DB·Redis 처리 여유를 더한 값이다. 처리가 끝나기 전에 키가 만료되어 중복 실행되는 일이 없도록 **TTL > 상한**을 유지한다.

## 5. 요청당 토큰 상한
- 입력: `@Size`로 keyword ≤ 50자, meaning·topic ≤ 100자
- 출력: `max-tokens: 300` 유지. 출력은 예문 한 문장 JSON이므로 충분하다. 너무 줄이면 JSON이 잘려 파싱에 실패할 수 있다.

## 6. 프론트엔드 규칙
- **멱등성 키 생성**: 사용자 액션(검색 버튼 클릭) 1회마다 `crypto.randomUUID()`로 새 키를 만든다.
- **재시도 시 재사용**: 네트워크 오류, 응답 타임아웃, 409 `REQUEST_IN_PROGRESS` 때문에 **같은 요청을 다시 보낼 때는 같은 키를 재사용**한다. 409는 1~2초 뒤에 다시 시도한다.
- **새 키를 만드는 경우**: 사용자가 입력을 바꾸거나 다시 검색 버튼을 누르는 것은 새 액션이므로 새 키를 만든다. 503 이후의 재시도는 **새 키**를 쓴다. `AI_UNAVAILABLE`은 서버가 키를 삭제했으므로 같은 키도 되지만, `TEMPORARILY_UNAVAILABLE`(저장된 응답 복원 실패)은 키가 24h 남아 같은 키로는 계속 503이 나기 때문이다. 규칙을 단순하게 하려고 503이면 모두 새 키로 통일한다.
- **대기 UX**: 요청 중에는 버튼을 비활성화하고 로딩을 표시한다. 클라이언트 타임아웃은 서버 상한보다 길게(예: 25s) 둔다.
- **에러 분기**: `code` 값으로 메시지를 나눈다. `USAGE_EXHAUSTED`는 한도 안내, `AI_UNAVAILABLE`·`TEMPORARILY_UNAVAILABLE`은 "잠시 후 다시 시도"를 보여준다.

## 7. 구현 단계
앞 단계가 뒤 단계의 기반이 된다. 예외 분류가 있어야 환불 조건과 키 삭제 조건을 정할 수 있다.

1. **에러 기반**
   - 전용 예외(`UsageExhaustedException`, `AiUnavailableException` 등)와 공통 에러 바디
   - `CustomControllerAdvice`의 `MethodArgumentNotValidException` import 수정
   - `GET` → `POST` + `@RequestBody` DTO + `@Size` 검증
   - OpenAI 예외와 파싱 실패를 `AiUnavailableException`으로 변환
2. **타임아웃·재시도**: `application.yml`에 `spring.ai.retry.*`, `spring.http.clients.*` 설정과 예산 식 주석을 추가한다. connect 시간을 실측하고, 4장의 방법으로 적용 여부를 확인한다.
3. **선차감 + 환불**: 호출 전에 `consumeUsage`를 커밋하고, 실패 시 환불용 조건부 UPDATE를 실행. `possibleUsage` 사전 확인은 제거
4. **멱등성 키**: Redis 상태(`IN_PROGRESS` / `DONE`), 409 처리, 실패 시 키 삭제

각 단계가 끝나면 `docs/API.md`의 "예문 생성" 항목(메서드, 요청, 오류 표)과 CLAUDE.md의 "AI 예문 생성과 사용량" 절을 갱신한다.

## 8. 테스트 관점
- 서비스 계층(`MockitoExtension`)
  - OpenAI 실패 시 환불과 키 삭제가 호출되는지
  - 429·401에서는 재시도하지 않는지
  - 데드라인이 부족하면 재시도를 건너뛰는지
- 웹 계층(`@WebMvcTest`)
  - 검증 실패 400, 키 누락 400, 409, 429, 503의 응답 바디 `code`
- 동시성: 잔량 1에서 동시에 요청하면 OpenAI가 1회만 호출되는지(실제 DB가 필요하므로 수동 또는 통합 테스트)

## 9. 감수 항목과 후속 과제
- 같은 키에 다른 요청 내용이 오면 이전 응답을 돌려준다 → ADR-006
- 단어 저장은 성공했는데 `DONE` 기록이 실패하는 경우 → ADR-006
- 키 삭제·`DONE` 기록 시 소유권을 확인하지 않음(처리가 30s를 넘길 때만 문제) → ADR-006
- 저장된 `DONE` 응답이 깨지면 그 키는 24h 동안 503 → ADR-005 (프론트는 새 키로 재시도)
- 선차감과 환불 사이에 서버가 죽으면 사용량 1회 손실 → ADR-003
- 스트리밍, 비동기 폴링, 전체 비용 상한 → ADR-001, ADR-007
- **후속**: 재시도 직전 데드라인 확인. `RetryTemplate` 빈을 직접 등록한다. 자동 설정의 `includes(TransientAiException, ResourceAccessException)`을 다시 써야 하고, `timeout` 설정이 필요하다 → ADR-004
- **확정 (2026-11-24)**: 2단계에서 Redis 자체가 장애일 때는 fail-closed(503 `TEMPORARILY_UNAVAILABLE`)로 막는다. 멱등성 없이 진행하면 중복 과금 보호가 사라지기 때문이다 → ADR-005
- **미정**: 단어 저장 실패의 상태 코드(현재 처리하지 않은 예외로 500)
