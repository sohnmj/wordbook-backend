# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew compileJava                 # fast syntax/type check
./gradlew build                       # compile + test + jar
./gradlew bootRun                     # run locally (needs env vars + MySQL + Redis)
./gradlew test                        # all tests
./gradlew test --tests "wordbook.backend.mail.MailServiceTest"          # single class
./gradlew test --tests "wordbook.backend.mail.MailServiceTest.sendMail" # single method
```

Docker: `./gradlew bootJar` produces `build/libs/*SNAPSHOT.jar`, which the `Dockerfile` copies. `docker compose up` starts Redis + the published `wellsohn/wordbook` image and reads secrets from the root `.env`. For local dev, `docker compose up my-redis` alone is usually enough (MySQL is expected to be external, reachable via `host.docker.internal`).

`src/main/resources/application.properties` is **gitignored** and reads everything from the environment: `DB_URL`, `DB_PASSWORD`, `JWT_SECRET`, `OPENAI_API_KEY`, `MAIL_HOST`, `MAIL_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URL`, `REDIRECT_CLIENT_URL`, `SPRING_DATA_REDIS_HOST`.

The project path contains Korean characters; shell output may render it mojibake, but the wrapper commands work fine.

## Stack — read before touching imports

Spring Boot **4.0.1** on Java 17, Gradle wrapper 9.3.0, Spring AI **2.0.0-M2**. These are pre-GA versions whose package layout differs from Boot 3, and the code already uses the new locations. Do not "correct" them to the familiar Boot 3 names:

- `tools.jackson.databind.ObjectMapper` / `tools.jackson.core.type.TypeReference` (Jackson 3), not `com.fasterxml.jackson.*`
- `org.springframework.boot:spring-boot-starter-webmvc` (webflux is also on the classpath)
- Test annotations from `org.springframework.boot.webmvc.test.autoconfigure.*` (`@WebMvcTest`, `@AutoConfigureMockMvc`), and `@MockitoBean` from `org.springframework.test.context.bean.override.mockito`
- `PathPatternRequestMatcher.withDefaults().matcher(...)` for security request matchers

## Package layout

Controllers are centralized in `wordbook.backend.controller`; everything else is grouped by aggregate under `wordbook.backend.domain.<aggregate>/{dto,entity,repository,service}` (`user`, `word`, `wordbook`, `wordbookword`, `wordbooklike`, `wordbookcomment`, `refresh`). Cross-cutting concerns sit at the top level: `security/{filter,handler,util}`, `redis`, `mail`, `usage`, `api`, `generator`, `config`.

Services never take another aggregate's repository — they depend on the other aggregate's *service* (e.g. `WordBookService` → `UserService.findUserByUsername`). Keep that direction.

## Authentication

Stateless JWT, session policy `STATELESS`, CSRF disabled.

- **Form login**: `LoginFilter` (installed at `UsernamePasswordAuthenticationFilter`) handles `POST /login` but reads a **JSON** body (`{"username","password"}`), not form params. `LoginSuccessHandler` returns `{"accessToken": "..."}` in the body and sets the refresh token as an httpOnly cookie named `refresh`.
- **Per-request auth**: `JWTFilter` (before `LoginFilter`) reads `Authorization: Bearer <token>`; a missing header passes through, a malformed one throws, an invalid/expired one returns 401 JSON.
- **Token typing**: `JWTUtil.createToken(username, role, isAccess)` stamps a `type` claim (`access`/`refresh`); `isValid(token, isAccess)` rejects a token used in the wrong slot. Always pass the right `isAccess` flag.
- **Refresh rotation**: `POST /api/v2/jwt/exchange` (`JWTService.refreshRotate`) reads the `refresh` cookie, validates it against the Redis whitelist key `user:<username>`, then issues a new pair.
- **Google OAuth2**: `CustomOAuth2UserService` upserts a user with username `GOOGLE_<sub>` and `isSocial=true`. `SocialSuccessHandler` only sets the refresh cookie and redirects to `app.oauth2.redirect-client-url` — the client must then call `/api/v2/jwt/exchange` to obtain an access token.
- **Public endpoints**: `POST` on `/api/v2/user/**`, `/api/v2/mail/**`, `/api/v2/jwt/**`. Everything else (including `GET /api/v2/user/remove`) requires authentication. Controllers get the caller via `Authentication#getName()`.

## Redis (three distinct responsibilities)

`RedisService` is the single wrapper around `StringRedisTemplate`:

1. **Email verification** — `wordbook:email:verify:<email>` holds the code (TTL 300s). On a correct code the value is overwritten with the literal `verify` for another 300s; `UserController.create` consumes and deletes it, so signup must finish inside that window.
2. **Refresh whitelist** — `user:<username>` → current refresh token, TTL 7 days.
3. **AI quota** — `getUsage` runs a Lua script atomically over `usage:<username>` (per-user daily claim lock, TTL 86400) and `wordbook:ai_api:limit` (global counter, self-initializing to 100). Returns `1` if the claim succeeded, `-1` if already claimed today or the global pool is exhausted.

Known inconsistency: `UsageScheduler` (cron `0 0 10 * * *`) resets a key named `API:usage`, which no longer matches the Lua script's `wordbook:ai_api:limit` (renamed in commit `01cbf9f`). The global counter is set without a TTL, so in practice it currently never refills.

## AI quota model (two tiers)

- `POST /api/v2/usage` → `UsageService.getUsage` claims the daily grant and adds **+10** to `UserEntity.usage` (a DB column).
- `GET /api/v2/openai/search` checks `UserService.possibleUsage` (usage > 0), calls the model, persists the word, then decrements `usage` by 1.

So Redis gates *claiming* the daily allowance; the MySQL `api_usage` column is the spendable balance.

## AI integration

`ApiService` is the interface; `ChatClientService` is the `@Primary` implementation over Spring AI's `ChatClient`. Its system prompt forces a strict JSON response which is parsed into `ApiResponseDTO` and mapped to `WordResponseDTO`. The model returns both `word` and `useword` (the inflected form actually used in the example); `WordService.getIndexFromExample` derives `exampleStartIndex`/`exampleLastIndex` from `useword` so the client can highlight it. Model config (`gpt-4o-mini`, temp 0.2, max 300 tokens) lives in `application.properties`.

## Persistence conventions

- `spring.jpa.hibernate.ddl-auto=update` with `PhysicalNamingStrategyStandardImpl` — there are no migrations, and **no** camelCase→snake_case conversion. Every column needs an explicit `@Column(name="...")`.
- All `@ManyToOne` are `LAZY`; `WordBookWordRepository.findWithWord` and `WordRepository.findTestWord` use explicit JPQL joins to avoid N+1. `findTestWord` orders by `function('RAND')` (MySQL-specific).
- `WordBookEntity` carries denormalized `likeCount`/`wordCount`, mutated through `increment*`/`decrement*` inside `@Transactional` service methods and flushed by dirty checking — no explicit `save()` call.
- Ownership checks are done in the query, not after loading: `findByIdAndUserEntity_Id(id, userId)`.
- Entities are Lombok `@Builder` + `@Getter` with no setters; mutation goes through named methods (`update`, `updateUsage`, `increment*`).

## Error handling

`CustomControllerAdvice` handles only `IllegalArgumentException` and — via an incorrect import (`org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException`) — a validation exception that MVC never throws, so `@Valid` failures on request bodies fall through to the default handler. Services throw bare `RuntimeException` for not-found cases. If you touch this area, fix the import to `org.springframework.web.bind.MethodArgumentNotValidException` rather than adding more handlers around it.

## Tests — current state

Treat `src/test` as exploratory scratch work, not a suite to keep green:

- Several `@SpringBootTest` classes hit a **live** MySQL/Redis and assume seeded data (hardcoded `username="admin1"`, `id=12L`), so they fail on a clean environment.
- Whole files are commented out (`RedisServiceTest`, `WordBookWordControllerTest`).
- `application-test.yml` defines an H2 `test` profile, but no H2 dependency is declared and no test activates the profile — that path does not work as written.
- `MailServiceTest` still asserts the pre-rename Redis prefix `email:` and therefore fails.

For new tests, follow `WordbookLikeControllerTest` (`@WebMvcTest` + `@MockitoBean` + `@WithMockUser`) for the web layer, or plain `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` for services. Test method names in Korean are the existing convention.

## Style

Comments and log messages are in Korean; keep that when editing existing files. Constructor injection is written by hand in most classes, with `@RequiredArgsConstructor` in the newer ones (`WordBookLikeService`, `WordBookCommentService`) — match the file you are in. All endpoints live under `/api/v2/`.
