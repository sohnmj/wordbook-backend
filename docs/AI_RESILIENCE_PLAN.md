# AI 예문 생성 안정화 플랜

OpenAI 호출(`/api/v2/openai/search`)의 에러 처리, 과금 보호, 대기 시간 상한 작업의 계획과 진행 상황이다.

- 결정 이유와 감수한 점: [ADR.md](./ADR.md) (ADR-001~007)
- 요청·응답·에러 코드 명세: [API.md](./API.md)의 "예문 생성"
- 프론트 전달 사항: [FRONTEND_CHANGES.md](./FRONTEND_CHANGES.md)

## 1. 배경: 작업 전 문제

| 문제 | 위치 (작업 전) | 영향 | 지금 |
|---|---|---|---|
| OpenAI 에러 처리 없음 | `ChatClientService.callApi` | 401·429·5xx·타임아웃·JSON 파싱 실패가 모두 500. 클라이언트가 원인을 구분할 수 없음 | 503 `AI_UNAVAILABLE`, 원인은 ERROR 로그 |
| 사용량 소진이 500 | `SearchController.search` (`IllegalStateException`) | 사용자에게 "한도 소진"을 알려줄 수 없음 | 429 `USAGE_EXHAUSTED` |
| 동시 요청 과금 구멍 | `possibleUsage` 확인 → OpenAI 호출 → `decreaseUsage` | 잔량 1에서 동시 N회 요청 시 N회 모두 OpenAI 과금 | 선차감(조건부 UPDATE) + 실패 시 환불 |
| 타임아웃 없음 + 공격적인 기본 재시도 | `application.yml` (`max-tokens: 300`만 존재) | HTTP 타임아웃이 없다. 여기에 Spring AI 기본 재시도(최대 10회, 2s×5배 백오프, 최대 180s 간격)가 겹쳐 검색 1회가 수십 분 걸리고 OpenAI 호출이 반복될 수 있음 | 재시도 1회, 최악 19s |
| 입력 길이 제한 없음 | `SearchController` 파라미터 | 긴 입력으로 입력 토큰 비용 증가 | `@Size` 적용 (`lang` 제외) |
| 부작용 있는 GET | `GET /search` | 단어 저장·사용량 차감이 GET으로 일어남. 재전송·프리페치에 취약 | `POST` + 멱등성 키 |
| `@Valid` 실패 미처리 | `CustomControllerAdvice` (messaging 패키지 import) | 검증 실패가 400으로 나가지 않음 | 400 `INVALID_INPUT` |

## 2. 범위

- **포함**: 에러 분류와 응답 표준화, 타임아웃·재시도와 20초 상한, 선차감 + 환불, 입력 길이 제한, 멱등성 키, `GET` → `POST`
- **제외**: 스트리밍(ADR-001), 비동기 처리·폴링, 서비스 전체 비용 상한(ADR-007)

## 3. 요청 흐름

`POST /api/v2/openai/search`, 조립은 `search.SearchService`(`@Transactional` 없음)

1. **입력 검증**: body `@Valid`, `Idempotency-Key` 헤더 UUID 검증 → 실패 시 400
2. **멱등성 키**: Redis `SET idem:<user>:<key> IN_PROGRESS NX EX 30`
   - `DONE` → 저장된 응답 반환 / `IN_PROGRESS` → 409 / Redis 장애 → 503 (ADR-005)
3. **선차감**: 조건부 UPDATE, 별도 트랜잭션으로 커밋. 잔량 없음 → 키 삭제 후 429 (ADR-003)
4. **OpenAI 호출**: 4장의 시간 예산 (ADR-004)
5. **단어 저장 → `DONE` 기록**(응답 JSON, TTL 24h). 기록 실패는 WARN 후 200 (ADR-006)
6. **4·5 실패 시**: 환불 + 키 삭제 후 원래 예외. AI 실패는 503, 저장 실패는 현재 500

## 4. 시간 예산

| 구간 | 값 |
|---|---|
| 1차 시도: 연결 + 응답 | 2s + 7s = 9s |
| 백오프 | 1s |
| 2차 시도: 연결 + 응답 | 2s + 7s = 9s |
| **합계** | **19s ≤ 20s** |

식: `(max-attempts + 1) × (connect + read) + backoff ≤ 20s`. 값을 바꿀 때 다시 계산한다. 재시도는 Spring AI 내장 `RetryTemplate` 한 곳에서만 한다.

```yaml
spring:
  ai:
    retry:
      max-attempts: 1            # 재시도 1회 = 최대 2번 시도
      on-client-errors: false    # 4xx(429 포함)는 재시도하지 않음
      backoff:
        initial-interval: 1s
  http:
    clients:
      connect-timeout: 2s
      read-timeout: 7s
```

멱등성 키 `IN_PROGRESS` TTL(30s)은 항상 이 상한보다 길어야 한다.

jar로 확인한 기본값과 문서 확인이 필요한 항목은 [notes/spring-ai-retry.md](./notes/spring-ai-retry.md)에 있다.

## 5. 진행 상황 (2026-11-27 기준)

**1. 에러 기반** (master 머지, PR #8)
- [x] 전용 예외(`UsageExhaustedException`, `AIUnavailableException` 등)와 공통 에러 바디
- [x] `CustomControllerAdvice`의 `MethodArgumentNotValidException` import 수정
- [x] `GET` → `POST` + `@RequestBody` DTO
- [ ] `@Size` 검증: keyword·meaning·topic 완료, **`lang` 미적용**
- [x] OpenAI 예외와 파싱 실패를 `AIUnavailableException`으로 변환

**2. 타임아웃·재시도** (master 머지, PR #8)
- [x] `application.yml`에 `spring.ai.retry.*`, `spring.http.clients.*` 설정
- [ ] `application.yml`에 예산 식 주석
- [ ] 운영 서버에서 connect 시간 실측
- [ ] read-timeout 1ms로 적용 여부 확인

**3. 선차감 + 환불** (master 머지, PR #8)
- [x] 호출 전에 `consumeUsage` 커밋, 실패 시 환불용 조건부 UPDATE
- [x] `possibleUsage` 사전 확인 제거
- [x] 환불 실패가 원래 예외를 덮지 않게 처리(suppressed)

**4. 멱등성 키** (`refactor` 브랜치, master 머지 전)
- [x] Redis 상태(`IN_PROGRESS` / `DONE`), 409 처리, 실패 시 키 삭제
- [x] Redis 장애 fail-closed(503), 헤더 UUID 검증
- [x] `SearchService` 분리, 코드 정리
- [x] 테스트: `SearchServiceTest`, `RedisServiceIdempotencyTest`, `SearchControllerTest`
- [x] 문서: ADR-005·006, `API.md`, CLAUDE.md, `FRONTEND_CHANGES.md`
- [ ] 프론트 반영 확인 후 master 머지
- [ ] 수동 확인: 같은 키 재요청 시 차감 없이 같은 응답, 동시 요청 시 409

### 확인 방법
- **connect 실측** (운영 서버): `curl -o /dev/null -s -w "connect=%{time_connect}s tls=%{time_appconnect}s\n" https://api.openai.com/v1/models`. 여유가 부족하면 connect 3s + read 6s로 바꾼다(시도당 9s 유지).
- **적용 확인** (로컬, 1~2회 과금): `read-timeout`을 `1ms`로 바꾸고 검색 1회 → `Retry error. Retry count:1` 로그 1번, 약 1s 뒤 503, ERROR 로그 cause에 `ResourceAccessException`. 확인 후 원래 값으로 되돌린다.

## 6. 남은 일과 후속 과제

**다음 작업**
- [ ] 프롬프트 `usedWord` ↔ JSON 형식 `useword` 불일치 (NPE 가능)
- [ ] `lang`에 `@Size`
- [ ] 단어 저장 실패의 상태 코드 결정 (현재 500)
- [ ] `HttpMessageNotReadableException` 로그에 사용자 입력이 섞이지 않게
- [ ] 동시성 확인: 잔량 1에서 동시 요청 시 OpenAI 1회만 호출 (실제 DB 필요)

**후속 과제**
- 재시도 직전 데드라인 확인 → ADR-004
- 같은 키에 다른 요청 내용 검사(요청 해시) → ADR-006
- `DONE` 기록 실패 대비, 키 소유권 확인 → ADR-006
- `UsageScheduler` 키 이름 버그, OpenAI 대시보드 월 한도 → ADR-007
