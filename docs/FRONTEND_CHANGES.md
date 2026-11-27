# 프론트엔드 변경 요청: 예문 생성 API

백엔드의 예문 생성(OpenAI) 안정화 작업으로 프론트에서 바꿔야 할 것을 모았다. 자세한 명세는 `docs/API.md`의 "예문 생성" 절을 참고한다.

| 구분 | 내용 | 백엔드 반영 |
|---|---|---|
| 1 | `GET` → `POST`, query → JSON body | master 머지 완료 (PR #8) |
| 2 | 에러 응답 형식 `{code, message}` | master 머지 완료 (PR #8) |
| 3 | `Idempotency-Key` 헤더 필수 | `refactor` 브랜치, master 머지 전 |

3번은 배포되는 순간 헤더가 없는 검색 요청이 모두 400이 된다. **프론트 반영 일정을 맞춘 뒤 머지한다.**

---

## 1. 요청 방식 변경

```
변경 전: GET  /api/v2/openai/search?keyword=apple&lang=en&meaning=...&topic=...
변경 후: POST /api/v2/openai/search
```

```http
POST /api/v2/openai/search
Authorization: Bearer {accessToken}
Content-Type: application/json
Idempotency-Key: 3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b

{ "keyword": "apple", "lang": "en", "meaning": "사과", "topic": "과일" }
```

| 필드 | 필수 | 제한 |
|---|---|---|
| keyword | O | 공백 불가, 최대 50자 |
| lang | O | 공백 불가 |
| meaning | X | 최대 100자 |
| topic | X | 최대 100자 |

- 입력 칸에 `maxlength`를 같은 값으로 걸어 두면 400을 미리 막을 수 있다.
- 성공 응답(200, `WordResponse`)의 형식은 바뀌지 않았다.

## 2. 에러 응답 형식

예문 생성의 에러는 모두 아래 형식이다. **분기는 HTTP 상태가 아니라 `code`로 한다**(503이 두 종류다).

```json
{ "code": "USAGE_EXHAUSTED", "message": "일일 이용 횟수를 모두 소진하였습니다." }
```

| 상태 | code | 상황 | 사용량 | 프론트 처리 |
|---|---|---|---|---|
| 400 | `INVALID_INPUT` | 입력 검증 실패, 헤더 누락·형식 오류 | 변화 없음 | `message`를 입력 오류로 표시. 재시도하지 않는다. |
| 409 | `REQUEST_IN_PROGRESS` | 같은 키의 요청이 처리 중 | 변화 없음 | 1~2초 뒤 **같은 키**로 재시도 |
| 429 | `USAGE_EXHAUSTED` | 일일 사용량 소진 | 변화 없음 | 한도 안내. 재시도하지 않는다. |
| 503 | `AI_UNAVAILABLE` | OpenAI 호출·응답 처리 실패 | 환불됨 | "잠시 후 다시 시도". 재시도는 **새 키** |
| 503 | `TEMPORARILY_UNAVAILABLE` | 서버 일시 장애 | 변화 없음 | "잠시 후 다시 시도". 재시도는 **새 키** |
| 500 | (Spring 기본 에러 JSON, `code` 없음) | 단어 저장 실패 | 환불됨 | 일반 오류로 표시 |

- 400 검증 실패의 `message`는 `필드: 사유` 형식이고, 여러 개면 `, `로 이어진다. 예: `keyword: 단어를 입력해 주세요., lang: 언어를 선택해 주세요.`
- 변경 전에는 사용량 소진·OpenAI 실패가 모두 500이었다. 500으로 사용량 소진을 판단하던 코드가 있다면 `code === "USAGE_EXHAUSTED"`로 바꾼다.
- 실패하면 사용량이 환불되거나 아예 차감되지 않는다. 실패 뒤에 잔여 사용량을 다시 조회하면 정확한 값이 나온다.

### 다른 API에도 영향이 있다
`@Valid` 검증을 하는 아래 API는 검증 실패 응답이 Spring 기본 에러 JSON에서 `{code: "INVALID_INPUT", message}`로 바뀌었다. body가 JSON이 아닐 때도 같다.

- `POST /api/v2/mail/**` (메일 발송, 인증 코드 확인)
- `POST /api/v2/user/**` (회원가입, 중복 확인)
- `POST /api/v2/wordbook` (단어장 생성)

에러 본문의 `error`·`status` 같은 Spring 기본 필드를 읽고 있었다면 확인이 필요하다.

## 3. `Idempotency-Key` 헤더 (master 머지 전)

같은 검색이 네트워크 재전송 등으로 두 번 실행되어 **사용량이 두 번 차감되는 것**을 막는 헤더다.

### 규칙
- **형식**: UUID (`crypto.randomUUID()`). 대소문자는 구분하지 않는다. 형식이 틀리거나 헤더가 없으면 400이다.
- **새 키를 만들 때**: 사용자 액션 1회(검색 버튼 클릭, 입력 변경 후 재검색)마다 만든다.
- **같은 키를 쓸 때**: 같은 요청을 다시 보낼 때. 네트워크 오류, 클라이언트 타임아웃, 409가 이 경우다.
- **새 키로 재시도할 때**: 503(`AI_UNAVAILABLE`, `TEMPORARILY_UNAVAILABLE`) 뒤에는 새 키로 재시도한다.
- **24시간 동안 저장**: 같은 키로 24시간 안에 다시 보내면, 서버는 다시 처리하지 않고 첫 응답(200)을 그대로 돌려준다. 이때 사용량은 차감되지 않는다.
- **다른 검색에 같은 키를 쓰지 않는다**: 서버는 요청 내용을 비교하지 않으므로, 이전 검색 결과가 그대로 돌아온다.

### 예시
```ts
async function searchExample(body: SearchRequest): Promise<WordResponse> {
  const key = crypto.randomUUID();              // 버튼 클릭 1회 = 키 1개

  for (let attempt = 0; attempt < 3; attempt++) {
    let res: Response;
    try {
      res = await fetch("/api/v2/openai/search", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
          "Idempotency-Key": key,
        },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(25_000),     // 서버 상한(20초)보다 길게
      });
    } catch {
      continue;                                  // 네트워크 오류·타임아웃: 같은 키로 재시도
    }

    if (res.ok) return res.json();

    const err = await res.json().catch(() => null);
    if (err?.code === "REQUEST_IN_PROGRESS") {
      await new Promise((r) => setTimeout(r, 1500));
      continue;                                  // 처리 중: 같은 키로 재시도
    }
    throw err;                                   // 400·429·503 등은 호출한 쪽에서 code로 처리
  }
  throw new Error("검색 요청이 계속 실패했습니다.");
}
```

## 4. 대기 UX
- 서버는 OpenAI 재시도를 포함해 **최대 약 20초** 안에 응답한다. 클라이언트 타임아웃은 이보다 길게(권장 25초) 둔다.
- 요청 중에는 검색 버튼을 비활성화하고 로딩을 표시한다. 중복 클릭으로 요청이 여러 번 나가는 것을 막는다.

## 체크리스트
- [ ] 예문 검색을 `POST` + JSON body로 변경
- [ ] keyword 50자, meaning·topic 100자 입력 제한
- [ ] 에러 처리를 `code` 기준으로 변경 (`USAGE_EXHAUSTED`, `AI_UNAVAILABLE`, `TEMPORARILY_UNAVAILABLE`, `REQUEST_IN_PROGRESS`, `INVALID_INPUT`)
- [ ] 메일·회원가입·단어장 생성의 검증 실패 응답 형식 확인
- [ ] 검색 버튼 클릭마다 `Idempotency-Key` 생성, 재시도 규칙 적용 (3번, 백엔드 머지 전)
- [ ] 클라이언트 타임아웃 25초, 요청 중 버튼 비활성화
