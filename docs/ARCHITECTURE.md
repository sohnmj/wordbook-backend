# Architecture

이 문서는 `CLAUDE.md`에 기술된 내용을 기반으로 wordbook-backend의 시스템 구성, 레이어 구조, 도메인 모델, 핵심 흐름을 시각화한다.

## 1. 시스템 구성도

```mermaid
flowchart TD
    Client["Client"]

    subgraph App["Spring Boot Application (/api/v2)"]
        Filter["JWTFilter / LoginFilter"]
        Controller["Controllers"]
        Service["Domain Services"]
    end

    MySQL["MySQL\n(UserEntity.usage 등 영속 데이터)"]

    subgraph Redis["Redis"]
        RedisAuthCode["인증코드\nwordbook:email:verify:<email>\nTTL 300s"]
        RedisAuthToken["인증토큰(Refresh Whitelist)\nuser:<username>\nTTL 7일"]
        RedisQuota["AI 쿼터\nusage:<username> / wordbook:ai_api:limit\nLua Script"]
    end

    subgraph External["External API"]
        OpenAI["OpenAI gpt-4o-mini\n(Spring AI ChatClient)"]
        GoogleOAuth["Google OAuth2"]
        MailServer["Mail Server"]
    end

    Client -->|"Authorization: Bearer / JSON body"| Filter
    Filter --> Controller
    Controller --> Service

    Service -->|"JPA (ddl-auto=update)"| MySQL
    Service -->|"이메일 인증코드 검증/저장"| RedisAuthCode
    Service -->|"refresh 토큰 화이트리스트 조회/저장"| RedisAuthToken
    Service -->|"일일/전역 AI 쿼터 원자적 차감"| RedisQuota

    Service -->|"단어 검색/생성"| OpenAI
    Service -->|"소셜 로그인"| GoogleOAuth
    Service -->|"인증 메일 발송"| MailServer

    App -->|"accessToken(JSON) / refresh(httpOnly cookie)"| Client
```

**의도 및 데이터 흐름**: 모든 요청은 `JWTFilter → LoginFilter → Controller → Service` 순서로 진입하며, 인증은 완전히 stateless(JWT + Redis 화이트리스트)하게 처리된다. Redis는 단일 `RedisService` 안에서 세 가지 독립된 책임(이메일 인증코드, refresh 토큰 화이트리스트, AI 일일/전역 쿼터)을 TTL과 Lua 스크립트로 구분해 담당하고, MySQL은 사용자·단어장 등의 영속 데이터와 AI 쿼터의 스펀더블 잔액(`usage` 컬럼)을 보관한다. 외부 API(OpenAI, Google OAuth2, 메일 서버)는 각각 단어 검색, 소셜 로그인, 이메일 인증 흐름에서만 호출된다.

## 2. 레이어드 구조 및 패키지 매핑

```mermaid
flowchart TB
    subgraph Presentation["Presentation Layer"]
        Ctrl["wordbook.backend.controller.*"]
        Filt["wordbook.backend.security.filter.*"]
    end

    subgraph Application["Application / Service Layer"]
        UserSvc["domain.user.service"]
        WordSvc["domain.word.service"]
        WordBookSvc["domain.wordbook.service"]
        WordBookWordSvc["domain.wordbookword.service"]
        WordBookLikeSvc["domain.wordbooklike.service"]
        WordBookCommentSvc["domain.wordbookcomment.service"]
        RefreshSvc["domain.refresh.service"]
    end

    subgraph Domain["Domain / Persistence Layer"]
        UserRepo["domain.user.{entity,repository}"]
        WordRepo["domain.word.{entity,repository}"]
        WordBookRepo["domain.wordbook.{entity,repository}"]
        WordBookWordRepo["domain.wordbookword.{entity,repository}"]
        WordBookLikeRepo["domain.wordbooklike.{entity,repository}"]
        WordBookCommentRepo["domain.wordbookcomment.{entity,repository}"]
    end

    subgraph Infrastructure["Infrastructure Layer"]
        RedisInfra["redis (RedisService)"]
        MailInfra["mail"]
        GenInfra["generator"]
        ApiInfra["api.ChatClientService"]
    end

    Presentation --> Application

    UserSvc --> UserRepo
    WordSvc --> WordRepo
    WordBookSvc --> WordBookRepo
    WordBookWordSvc --> WordBookWordRepo
    WordBookLikeSvc --> WordBookLikeRepo
    WordBookCommentSvc --> WordBookCommentRepo

    WordBookSvc -.->|"타 애그리거트는 Service 경유\n(Repository 직접 참조 금지)"| UserSvc
    WordBookWordSvc -.->|"Service 간 협력"| WordSvc

    Application --> Infrastructure
```

**의도 및 데이터 흐름**: 패키지는 애그리거트(`user`, `word`, `wordbook`, `wordbookword`, `wordbooklike`, `wordbookcomment`, `refresh`) 단위로 `dto/entity/repository/service`를 함께 묶고, 횡단 관심사(`redis`, `mail`, `generator`, `api`, `security`)는 최상위에 둔다. 핵심 규칙은 서비스가 타 애그리거트의 Repository를 직접 참조하지 않고 반드시 그 애그리거트의 Service를 통해 협력하는 것이며(예: `WordBookService → UserService.findUserByUsername`), 점선 화살표가 이 경계를 나타낸다.

## 3. 도메인 ERD

```mermaid
erDiagram
    UserEntity ||--o{ WordBookEntity : "1:N (작성)"
    UserEntity ||--o{ WordBookLike : "1:N (좋아요)"
    UserEntity ||--o{ WordBookComment : "1:N (댓글)"
    WordBookEntity ||--o{ WordBookWord : "1:N"
    WordEntity ||--o{ WordBookWord : "1:N"
    WordBookEntity ||--o{ WordBookLike : "1:N"
    WordBookEntity ||--o{ WordBookComment : "1:N"

    UserEntity {
        Long id PK
        String username
        String password
        boolean isSocial
        int usage "AI 쿼터 스펀더블 잔액"
    }

    WordBookEntity {
        Long id PK
        Long userEntity_id FK
        String title
        int likeCount "비정규화 컬럼"
        int wordCount "비정규화 컬럼"
    }

    WordEntity {
        Long id PK
        String word
        String meaning
        String example
    }

    WordBookWord {
        Long id PK
        Long wordBookEntity_id FK
        Long wordEntity_id FK
        int exampleStartIndex
        int exampleLastIndex
    }

    WordBookLike {
        Long id PK
        Long userEntity_id FK
        Long wordBookEntity_id FK
    }

    WordBookComment {
        Long id PK
        Long userEntity_id FK
        Long wordBookEntity_id FK
        String content
    }
```

**의도 및 데이터 흐름**: `WordBookWord`는 `WordBookEntity`와 `WordEntity`를 연결하는 N:M 관계의 실체 테이블로, AI가 반환한 `useword`(실제 사용된 활용형)로부터 계산된 `exampleStartIndex`/`exampleLastIndex`를 함께 들고 있어 클라이언트가 예문에서 해당 단어를 하이라이트할 수 있게 한다. `WordBookEntity`의 `likeCount`/`wordCount`는 `WordBookLike`/`WordBookWord` 증감 시 서비스 메서드(`increment*`/`decrement*`) 안에서 더티 체킹으로만 갱신되는 비정규화 컬럼이며, 모든 `@ManyToOne`은 `LAZY`이고 컬럼명은 `PhysicalNamingStrategyStandardImpl` 때문에 자동 스네이크케이스 변환 없이 명시적으로 지정된다.

## 4. 핵심 흐름 시퀀스 — AI 단어 검색 및 2-Tier 쿼터

```mermaid
sequenceDiagram
    participant Client
    participant Controller as "SearchController"
    participant UserService
    participant ChatClientService
    participant WordService
    participant MySQL as "UserEntity (MySQL)"

    Client->>Controller: "GET /api/v2/openai/search"
    Controller->>UserService: "possibleUsage(username)"
    UserService->>MySQL: "usage 컬럼 조회"
    MySQL-->>UserService: "usage > 0 ?"

    alt "usage <= 0"
        UserService-->>Controller: "false"
        Controller-->>Client: "쿼터 부족 응답"
    else "usage > 0"
        UserService-->>Controller: "true"
        Controller->>ChatClientService: "단어 검색 요청"
        ChatClientService->>ChatClientService: "gpt-4o-mini 호출\n(temp 0.2, max 300 tokens)"
        ChatClientService-->>Controller: "ApiResponseDTO (word, useword 등)"

        Controller->>WordService: "getIndexFromExample(useword, example)"
        WordService->>WordService: "exampleStartIndex/exampleLastIndex 계산"
        WordService->>MySQL: "WordEntity 저장"

        Controller->>UserService: "usage -1 차감"
        UserService->>MySQL: "usage 컬럼 갱신 (dirty checking)"

        Controller-->>Client: "WordResponseDTO 반환"
    end
```

**의도 및 데이터 흐름**: `POST /api/v2/usage`가 Redis Lua 스크립트로 일일 쿼터 "claim" 여부를 원자적으로 판정하고 성공 시 MySQL `usage` 컬럼에 +10을 적립하는 것과 별개로, 이 흐름은 그 적립된 MySQL 잔액을 실제로 "소비"하는 단계다. `UserService.possibleUsage`가 잔액을 확인해 게이트 역할을 하고, `ChatClientService`가 Spring AI `ChatClient`로 OpenAI를 호출해 엄격한 JSON 응답을 `ApiResponseDTO`로 파싱하며, `WordService`가 `useword` 기준으로 하이라이트 인덱스를 계산해 저장한 뒤 마지막으로 `usage`가 1 차감된다. 즉 Redis는 "하루치 권리 획득"을, MySQL `usage` 컬럼은 "실제 호출 시 차감되는 잔액"을 담당하는 2-Tier 구조다.
