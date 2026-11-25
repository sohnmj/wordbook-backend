# Wordbook API 명세

현재 코드(`github_action` 브랜치) 기준으로, 실제 동작 그대로 적었다.

## 공통

- Base URL: 서버 주소. 엔드포인트는 `/api/v2/` 아래에 있다. 예외: `/login`, `/healthcheck/env`, OAuth2 경로.
- 인증: `Authorization: Bearer {accessToken}`
- Content-Type: 요청과 응답 모두 `application/json`. 예외로 표시한 곳만 다르다.
- 에러 응답은 형식이 통일되어 있지 않다. 예문 생성과 입력 검증 실패만 `{code, message}` 형식이고, 나머지는 `code` 필드가 없다.

| 상황 | 상태 | 본문 |
|---|---|---|
| `IllegalArgumentException` | 400 | `잘못된 요청입니다.` (text) |
| `@RequestBody @Valid` 검증 실패, body가 JSON이 아님 | 400 | `{"code":"INVALID_INPUT","message":"<필드>: <사유>"}` |
| 필수 헤더 누락 | 400 | `{"code":"INVALID_INPUT","message":"<헤더명> 헤더가 필요합니다."}` |
| `CustomException` 계열(예문 생성) | 예외별 | `{"code":"...","message":"..."}` (예문 생성 절 참고) |
| 필수 query 파라미터 누락 | 400 | Spring 기본 에러 JSON |
| 액세스 토큰 무효·만료 | 401 | `{"error":"토큰 만료 또는 유효하지 않은 토큰"}` |
| `Authorization` 헤더가 `Bearer `로 시작하지 않음 | 500 | Spring 기본 에러 JSON |
| 토큰 없이 인증 필요 경로 호출 | 실측 필요 | oauth2Login 기본 진입점이 응답한다(리다이렉트 가능성 있음) |
| 그 외 처리하지 않은 예외(`RuntimeException`, `IllegalStateException`, `UsernameNotFoundException`) | 500 | Spring 기본 에러 JSON |

---

## 인증

### 로그인
POST /login
인증: 없음

[요청] body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| username | String | O | 앞뒤 공백 제거 |
| password | String | O | 앞뒤 공백 제거 |

[응답] 200

| 필드 | 타입 | 설명 |
|---|---|---|
| accessToken | String | 액세스 토큰 |

- `Set-Cookie: refresh={refreshToken}`가 함께 내려온다. 속성: HttpOnly, Secure, Path=/, Max-Age=86400.
- 같은 refresh 토큰이 Redis `user:{username}`에도 저장된다(TTL 7일).

[오류]

| 상태 | 상황 |
|---|---|
| 401 | 아이디 또는 비밀번호 불일치 |
| 500 | body가 JSON이 아님 |

### 토큰 재발급
POST /api/v2/jwt/exchange
인증: 없음. 쿠키 `refresh`가 필요하다.

[요청] body 없음. 쿠키 `refresh`를 보낸다.

[응답] 200

| 필드 | 타입 | 설명 |
|---|---|---|
| accessToken | String | 새 액세스 토큰 |

- 새 refresh 토큰이 `Set-Cookie: refresh`로 교체된다. 속성: HttpOnly, Path=/, Max-Age=86400, **Secure 없음**.
- Redis 화이트리스트 값도 새 토큰으로 바뀐다.

[오류]

| 상태 | 상황 |
|---|---|
| 400 | Redis에 refresh 토큰이 없음, 또는 저장된 값과 다름 |
| 500 | 쿠키 없음, `refresh` 쿠키 없음, 토큰 서명·만료·타입 오류 |

### Google 로그인
GET /oauth2/authorization/google
인증: 없음

[요청] 없음. 브라우저에서 이 주소로 이동시킨다.

[응답] 302

- 로그인에 성공하면 `app.oauth2.redirect-client-url`로 리다이렉트된다.
- 이때 쿠키 `refresh`가 설정된다. 속성: HttpOnly, Path=/, **Max-Age=100**, Secure 없음.
- 클라이언트는 100초 안에 `/api/v2/jwt/exchange`를 호출해서 액세스 토큰을 받아야 한다.

---

## 회원

### 이메일 인증 코드 발송
POST /api/v2/mail
인증: 없음

[요청] body

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| email | String | O | 이메일 형식 |

[응답] 200, `true` (Boolean)

- 4자리 숫자 코드를 메일로 보낸다.
- Redis `wordbook:email:verify:{email}`에 코드를 저장한다(TTL 300초).

[오류]

| 상태 | 상황 |
|---|---|
| 400 | 검증 실패 |
| 500 | 메일 전송 실패 |

### 이메일 인증 코드 확인
POST /api/v2/mail/verify
인증: 없음

[요청] body

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| email | String | O | 이메일 형식 |
| code | String | O | 4자 |

[응답] 200, `true` 또는 `false` (Boolean)

- `true`이면 인증 완료 상태가 300초 동안 유지된다. 이 시간 안에 회원가입을 마쳐야 한다.

[오류]

| 상태 | 상황 |
|---|---|
| 400 | 검증 실패 |

### 회원가입
POST /api/v2/user
인증: 없음

[요청] body

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| username | String | O | 영문·숫자 5~10자 |
| password | String | O | 8~16자 |
| email | String | O | 이메일 형식. 사전에 인증을 마쳐야 함 |

[응답] 200, 생성된 사용자 id (Long)

[오류]

| 상태 | 상황 |
|---|---|
| 400 | 검증 실패, 또는 이메일 미인증 |
| 500 | username 또는 email 중복 (DB unique 제약 위반) |

### 아이디 중복 확인
POST /api/v2/user/exist
인증: 없음

[요청] body: 회원가입과 같은 형식이고, **세 필드 모두 검증한다**.

[응답] 200, 이미 존재하면 `true` (Boolean)

[오류]

| 상태 | 상황 |
|---|---|
| 400 | 검증 실패 |

### 회원 탈퇴
GET /api/v2/user/remove
인증: 필요

[요청] 없음

[응답] 200, 삭제된 username (String, text)

[오류]

| 상태 | 상황 |
|---|---|
| 500 | 사용자 없음 |

---

## AI 사용량

### 일일 사용량 받기
POST /api/v2/usage
인증: 필요

[요청] 없음

[응답] 200, `true` 또는 `false` (Boolean)

- `true`: 사용량 +10 지급.
- `false`: 오늘 이미 받았거나, 전역 한도를 모두 소진함.

[사용 제한] 사용자당 24시간에 1회. 전역 풀(100회)을 사용자 전체가 나눠 쓴다.

### 잔여 사용량 조회
GET /api/v2/usage
인증: 필요

[요청] 없음

[응답] 200, 잔여 횟수 (Integer)

### 예문 생성
POST /api/v2/openai/search
인증: 필요

[요청 헤더]

| 헤더 | 필수 | 설명 |
|---|---|---|
| Idempotency-Key | O | UUID(8-4-4-4-12 hex). 대소문자 구분 없음. 사용자 액션마다 새로 만들고, 같은 요청을 재시도할 때는 재사용한다. |

[요청] body (JSON)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| keyword | String | O | 대상 단어, 최대 50자 |
| lang | String | O | 대상 언어 |
| meaning | String | X | 원하는 의미, 최대 100자 |
| topic | String | X | 주제·상황, 최대 100자 |

[응답] 200, WordResponse (아래 공통 모델 참고)

- 생성된 단어는 사용자 단어로 저장된다.
- 사용량이 1 차감된다. OpenAI 호출이나 저장이 실패하면 환불된다.
- 같은 키로 24시간 안에 다시 요청하면, 차감·호출 없이 처음 응답을 그대로 돌려준다.

[오류] 본문은 `{"code": "...", "message": "..."}`

| 상태 | code | 상황 | 사용량 |
|---|---|---|---|
| 400 | `INVALID_INPUT` | body 검증 실패, JSON 아님, `Idempotency-Key` 누락·형식 오류 | 변화 없음 |
| 409 | `REQUEST_IN_PROGRESS` | 같은 키의 요청이 처리 중. 1~2초 뒤 같은 키로 재시도 | 변화 없음 |
| 429 | `USAGE_EXHAUSTED` | 일일 사용량 소진 | 변화 없음 |
| 503 | `AI_UNAVAILABLE` | OpenAI 호출 실패(타임아웃·5xx 등), 응답 JSON 파싱 실패 | 환불 |
| 503 | `TEMPORARILY_UNAVAILABLE` | Redis 장애, 저장된 응답 복원 실패. 새 키로 재시도 | 변화 없음 |
| 500 | (Spring 기본 에러 JSON) | 단어 저장 실패 | 환불 |

[사용 제한] 호출 1회에 사용량 1이 차감된다.

---

## 단어

### 내 단어 목록
GET /api/v2/word/list
인증: 필요

[요청] 없음

[응답] 200, WordResponse 배열

- 채워지는 필드: wordId, word, example, meaning, topic, lang.
- 나머지 필드는 null 또는 0이다.

---

## 단어장

### 단어장 생성
POST /api/v2/wordbook
인증: 필요

[요청] body

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| name | String | O | 공백 불가 |

[응답] 200, WordBookList (`count`는 null)

[오류]

| 상태 | 상황 |
|---|---|
| 400 | 검증 실패 |

### 내 단어장 목록
GET /api/v2/wordbook/mylist
인증: 필요

[요청] 없음

[응답] 200, WordBookList 배열

### 단어장 상세
GET /api/v2/wordbook
인증: 필요. 소유자 검사는 없다.

[요청] query

| 필드 | 타입 | 필수 |
|---|---|---|
| id | long | O |

[응답] 200

| 필드 | 타입 | 설명 |
|---|---|---|
| name | String | 단어장 이름 |
| likeCount | Long | 좋아요 수 |
| words | WordResponse[] | 채워지는 필드: wordId, word, lang, meaning, example, topic, translation |

[오류]

| 상태 | 상황 |
|---|---|
| 500 | 단어장 없음 |

### 단어장 삭제
DELETE /api/v2/wordbook
인증: 필요

[요청] query

| 필드 | 타입 | 필수 |
|---|---|---|
| id | long | O |

[응답] 200, 삭제한 id (Long)

[오류]

| 상태 | 상황 |
|---|---|
| 500 | 단어장이 없거나 본인 소유가 아님 |

### 전체 단어장 목록 (페이지)
GET /api/v2/wordbook/list
인증: 필요

[요청] query

| 필드 | 타입 | 필수 | 기본값 |
|---|---|---|---|
| page | int | X | 0 |
| size | int | X | 10 |
| sort | String | X | `id,desc` (좋아요 순은 `likeCount,desc`) |

[응답] 200, WordBookList 배열. 페이지 메타데이터는 없다.

---

## 단어장-단어

### 단어장에 단어 추가
POST /api/v2/wordbookword
인증: 필요

[요청] body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| word | Long | O | 단어 id |
| wordbook | Long | O | 단어장 id |

[응답] 200, 생성된 연결 id (Long)

[오류]

| 상태 | 상황 |
|---|---|
| 500 | 단어 또는 단어장이 없거나 본인 소유가 아님 |

### 단어장에서 단어 제거
DELETE /api/v2/wordbookword
인증: 필요

[요청] query

| 필드 | 타입 | 필수 |
|---|---|---|
| wordBookId | Long | O |
| wordId | Long | O |

[응답] 200, 삭제한 연결 id (Long)

[오류]

| 상태 | 상황 |
|---|---|
| 500 | 단어나 단어장, 연결이 없거나 본인 소유가 아님 |

### 시험 단어 조회
GET /api/v2/test
인증: 필요. 소유자 검사는 없다.

[요청] query

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| wordbookId | Long | O | 단어장 id |
| size | int | O | 개수 |

[응답] 200, WordResponse 배열 (무작위 순서)

- 채워지는 필드: word, lang, meaning, example, translation, useword, startIndex, lastIndex.
- wordId는 null이다.

---

## 좋아요

### 좋아요 토글
POST /api/v2/wordbooklike/{wordbook_id}
인증: 필요

[요청] path

| 필드 | 타입 | 필수 |
|---|---|---|
| wordbook_id | Long | O |

[응답] 200, Boolean

- `true`: 좋아요 추가됨.
- `false`: 좋아요 취소됨.

[오류]

| 상태 | 상황 |
|---|---|
| 500 | 단어장 없음 |

---

## 댓글

### 댓글 작성
POST /api/v2/wordbookcomment
인증: 필요

[요청] body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| comment | String | O | 내용 |
| wordBookId | Long | O | 단어장 id |
| parentId | Long | X | 대댓글이면 부모 댓글 id |

[응답] 200, 생성된 댓글 id (Long)

### 댓글 목록
GET /api/v2/wordbookcomment
인증: 필요

[요청] query

| 필드 | 타입 | 필수 |
|---|---|---|
| wordbookId | Long | O |

[응답] 200, 배열

| 필드 | 타입 | 설명 |
|---|---|---|
| comment | String | 내용 |
| userId | Long | 작성자 id |
| userName | String | 작성자 username |

> 두 댓글 API는 현재 항상 500이다. "구현상 주의" 1번을 참고.

---

## 기타

### 배포 색상 조회
GET /healthcheck/env
인증: 없음

[요청] 없음

[응답] 200, `local`, `blue`, `green` 중 하나 (text)

---

## 공통 모델

### WordResponse

| 필드 | 타입 | 설명 |
|---|---|---|
| wordId | Long | 단어 id |
| word | String | 원형 |
| meaning | String | 뜻 |
| lang | String | 언어 |
| example | String | 예문 |
| topic | String | 주제 |
| translation | String | 예문 번역(한국어) |
| useword | String | 예문에 실제로 쓰인 형태 |
| startIndex | int | 예문 안 `useword` 시작 위치 (없으면 -1) |
| lastIndex | int | 예문 안 `useword` 끝 위치 (해당 위치 포함) |

### WordBookList

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 단어장 id |
| name | String | 이름 |
| count | Long | 단어 수 |
| likeCount | Long | 좋아요 수 |

---

## 구현상 주의 (문서 작성 중 발견)

1. `WordBookCommentController`: 필드가 `final`이 아니어서 `@RequiredArgsConstructor`가 주입하지 않는다. 서비스가 null이라 호출하면 500이 난다.
2. `WordBookCommentRepository`: 쿼리 조건이 `parentEntity = :parent`인데 null을 넘긴다. JPQL에서 `= null`은 참이 되지 않으므로, 1번을 고쳐도 목록은 항상 빈 배열이다.
3. refresh 쿠키 설정이 경로마다 다르다.
   - 로그인: Secure, 1일.
   - 재발급: Secure 없음, 1일.
   - 소셜 로그인: Secure 없음, 100초.
   - Redis TTL은 7일이다.
4. 부작용이 있는 GET이 있다: `/user/remove`, `/openai/search`.
5. `/user/exist`는 username만 쓰는데 password와 email까지 필수로 검증한다.
6. 존재하지 않는 리소스, 권한 없음, 사용량 소진이 모두 500으로 응답된다. 클라이언트가 이 경우들을 구분할 수 없다.
