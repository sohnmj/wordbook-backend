# Spring AI 재시도·타임아웃 조사 기록

ADR-004 결정의 근거 자료다. Spring AI 2.0.0-M2 / Boot 4.0.1 jar의 설정 메타데이터와 바이트코드로 확인했다(2026-11-21).

## 확인된 사실
- Spring AI 재시도 기본값: `max-attempts=10`, `backoff.initial-interval=2s`, `multiplier=5`, `max-interval=180s`
- 재시도 구현: Spring Framework 7의 `org.springframework.core.retry.RetryTemplate`. `OpenAiChatModel`이 호출할 때 사용한다.
- 재시도 대상 예외: `TransientAiException`, `ResourceAccessException`(I/O 오류·타임아웃)
- HTTP 상태 분류 순서:
  1. `on-http-codes`에 있는 코드 → 재시도
  2. 4xx이고 `on-client-errors=false` → `NonTransientAiException`(재시도 안 함)
  3. `exclude-on-http-codes`에 있는 코드 → 재시도 안 함
  4. 나머지(5xx) → 재시도

  그래서 429는 기본 설정에서도 재시도되지 않는다.
- `RetryTemplate`과 `ResponseErrorHandler` 빈은 `@ConditionalOnMissingBean`이다. 직접 등록하면 자동 설정 대신 쓰인다(데드라인 확인 후속 과제에서 사용).
- HTTP 타임아웃 설정은 `spring.http.clients.connect-timeout`, `read-timeout`이다(단수형 `client`는 4.0.0부터 deprecated). Spring AI 쪽에는 타임아웃 설정이 없다.
- 그릴링 때 합의한 "연결 3s / 응답 10s / 총 20s"는 재시도 1회를 넣으면 최악 26s라서, 시도당 값을 2s/7s로 줄였다.

## 문서 확인 필요
- `max-attempts`가 `RetryPolicy.maxRetries`에 그대로 들어간다. "첫 시도를 뺀 재시도 횟수"로 보이지만 Framework 문서로 확인해야 한다.
- Spring AI의 `RestClient`가 `spring.http.clients.*` 타임아웃을 실제로 따르는지. 클라이언트 구현에 따라 connect 타임아웃에 TLS 핸드셰이크가 포함되는지도 달라진다(classpath에 Reactor Netty가 있다).
- read 타임아웃이 바이트 사이 간격인지 응답 전체 시간인지. 바이트 사이 간격이면 응답이 조금씩 계속 오는 경우 7s를 넘을 수 있다.
- `RetryPolicy.Builder.timeout(Duration)`의 의미(데드라인 확인에 쓸 수 있는지)
- OpenAI 429 에러 본문 형식(나중에 rate limit만 재시도하게 바꿀 경우)
