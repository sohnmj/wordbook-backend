# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 협업 방식 (최우선)

사용자는 **학습자**이고, Claude는 멘토로서 돕는다.

- 사용자가 "코드 짜줘"라고 하기 전에는 파일을 수정하지 않고, 완성 코드도 작성하지 않는다.
- 막히면 개념 → 힌트(봐야 할 클래스·설정) → 핵심만 담은 짧은 예시 순서로 돕는다.
- 리뷰할 때는 문제의 위치와 이유만 말하고, 수정은 사용자가 한다.
- 라이브러리나 OpenAI 스펙의 기본값은 추측하지 않고 "문서 확인 필요"라고 말한다.
- 한국어로 간결하게 답한다.

## 명령어

- `./gradlew compileJava`: 빠른 컴파일 확인
- `./gradlew build -x test`: CI와 같은 빌드
- `./gradlew test --tests "<FQCN>[.<method>]"`: 단일 테스트 실행
- `./gradlew bootRun`: 로컬 실행. 환경변수와 외부 MySQL, Redis가 필요하다.
- `docker compose up my-redis`: 로컬 Redis 실행

## 실행 환경

- `application.yml` 하나에 프로필 그룹 `local|blue|green` + `common` + `secret`이 들어 있다. 비밀값은 모두 환경변수에서 읽는다.
- CI(`.github/workflows/CICD.yml`)는 master에 push될 때 테스트 없이 빌드하고 Docker Hub에 푸시한다.
- 그다음 `/healthcheck/env`로 현재 색을 확인하고 blue(8080)/green(8081)을 전환해 무중단 배포한다.

## 스택 주의

Spring Boot **4.0.1**, Spring AI **2.0.0-M2**, Java 17. 둘 다 Boot 3와 패키지 위치가 다르다. 아래 이름을 Boot 3 시절 이름으로 "고치지" 말 것.

- Jackson 3: `tools.jackson.*`
- 테스트 어노테이션: `org.springframework.boot.webmvc.test.autoconfigure.*`, `@MockitoBean`
- 시큐리티 매처: `PathPatternRequestMatcher`

webmvc와 webflux가 둘 다 classpath에 있다.

## 구조

- 컨트롤러는 `controller`에 모여 있다.
- 도메인은 `domain.<aggregate>/{dto,entity,repository,service}` 아래에 있다.
- 공통 관심사는 최상위 패키지 `security`, `redis`, `mail`, `usage`, `api`, `config`에 있다.
- 서비스는 다른 aggregate의 repository를 직접 쓰지 않고, 그 aggregate의 **서비스**를 통해 접근한다.
- 엔티티에는 setter가 없다. 변경은 이름 있는 메서드로 하고, 저장은 dirty checking에 맡긴다.
- `ddl-auto=update`이고 네이밍 전략이 `PhysicalNamingStrategyStandardImpl`이다. 따라서 모든 컬럼에 `@Column(name=...)`을 명시해야 한다.
- 소유권 검사는 쿼리 조건으로 한다(`findByIdAndUserEntity_Id`).

## 인증

- 세션 없는 JWT 방식이다.
- 로그인: `LoginFilter`가 JSON body를 받는다. access token은 응답 body로, refresh token은 httpOnly 쿠키 `refresh`로 내려준다.
- `JWTUtil`은 토큰에 `type` 클레임(access/refresh)을 넣는다. 검증할 때 `isAccess` 플래그를 맞게 넘겨야 한다.
- refresh 토큰은 Redis 화이트리스트 `user:<username>`(TTL 7일)에 저장되고, `/api/v2/jwt/exchange`에서 교체(rotate)된다.
- Google OAuth2 로그인은 refresh 쿠키만 심고 리다이렉트한다. 클라이언트가 이후 exchange를 호출해야 access token을 받는다.

## AI 예문 생성과 사용량

- 호출 경로: `POST /api/v2/openai/search`(헤더 `Idempotency-Key` 필수, UUID) → `search.SearchService`가 멱등성 키 → 선차감 → AI 호출 → 단어 저장 → `DONE` 기록 순으로 조립한다. 실패하면 환불하고 키를 지운다. `SearchService`에는 `@Transactional`을 붙이지 않는다.
- `ChatClientService`가 Spring AI `ChatClient`를 **동기**로 호출한다. 응답은 엄격한 JSON이고 실패는 `AIUnavailableException`(503)으로 바꾼다.
- 멱등성 키: Redis `idem:<user>:<key>`, 값은 `IN_PROGRESS`(30s) 또는 `DONE:<응답 JSON>`(24h). Redis 장애 시 503(fail-closed).
- 타임아웃·재시도는 `application.yml`의 `spring.ai.retry.*`, `spring.http.clients.*`로만 설정한다(최대 1회 재시도, 최악 19s).
- 설계 결정은 `docs/ADR.md`, 전체 계획은 `docs/AI_RESILIENCE_PLAN.md`에 있다.
- 사용량은 두 단계다.
  - Redis Lua(`RedisService.getUsage`): 하루 1회 claim을 처리한다. 키는 `usage:<user>`와 전역 키 `wordbook:ai_api:limit`이다.
  - claim에 성공하면 DB `usage`에 +10을 더한다. 검색 1회마다 조건부 UPDATE로 1씩 차감한다.
- 후속 과제: 재시도 직전 데드라인 확인, 단어 저장 실패의 상태 코드, 비동기 처리·폴링(ADR-007).

## 알려진 문제

- `UsageScheduler`는 `API:usage` 키를 리셋한다. Lua 스크립트가 쓰는 키(`wordbook:ai_api:limit`)와 이름이 달라서 전역 한도가 다시 채워지지 않는다.
- `src/test`는 실험용이다. 실제 DB와 Redis, 시드 데이터에 의존하고, 일부는 주석 처리돼 있다. H2 `test` 프로필은 동작하지 않는다.
  - 새 테스트는 웹 계층이면 `WordbookLikeControllerTest` 방식(`@WebMvcTest`)을 따른다.
  - 서비스 계층이면 `MockitoExtension`을 쓴다.

## 스타일

- 주석, 로그, 테스트 메서드 이름은 한국어로 쓴다.
- 생성자 주입 방식은 파일마다 다르다. 수동 작성과 `@RequiredArgsConstructor`가 섞여 있으니 해당 파일 방식을 따른다.
- 엔드포인트는 모두 `/api/v2/` 아래에 둔다.

## 문서

- ADR 결정을 바꿀 때 이전 결정은 지우지 말고 변경 이력으로 남긴다.
