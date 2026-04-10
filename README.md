# auth-rbac-admin

`auth-rbac-admin`은 Spring Boot 기반 관리자 백엔드 포트폴리오 프로젝트입니다.  
local JWT 인증, Redis refresh token, RBAC, 감사 로그를 직접 구현하고, 여기에 Keycloak 기반 OIDC / SSO access token까지 기존 내부 권한 구조에 연결할 수 있도록 확장한 형태입니다.

## 1. 프로젝트 소개

이 프로젝트는 단순 CRUD 예제가 아니라 인증 이후 인가와 감사 추적까지 이어지는 관리자 백엔드의 핵심 흐름을 구현하는 데 초점을 맞췄습니다.

- 관리자 계정 local 로그인과 JWT 발급
- Redis 기반 refresh token / reissue / logout
- Role, Menu, Permission, User 관리
- 요청 경로와 HTTP Method 기반의 실제 API 인가
- 관리자 작업 이력을 남기는 Audit 로그
- Keycloak 기반 OIDC / SSO access token 수용
- 테스트 코드까지 포함한 실무형 백엔드 포트폴리오

## 2. 기술 스택

- Java 21
- Spring Boot 3
- Gradle
- Spring Security
- Spring Data JPA
- Spring Data Redis
- OAuth2 Resource Server
- H2
- Redis
- JWT
- JUnit5
- MockMvc

## 3. 핵심 기능

### Role 관리
- 역할 생성, 목록 조회, 단건 조회, 수정, 논리 삭제

### Menu 관리
- 메뉴 생성, 목록 조회, 단건 조회, 수정, 논리 삭제
- `parentId` 기반 트리 응답 조회
- `routePath / apiPath` 분리 관리

### Permission 관리
- `roleId + menuId` 조합 기반 권한 관리
- `READ / CREATE / UPDATE / DELETE` 액션 플래그 관리
- `menu.apiPath` 기준 실제 API 인가

### User 관리
- 로그인 계정, 비밀번호, 역할, 활성화 상태 관리
- 비밀번호 BCrypt 저장
- soft delete 이후에도 `loginId` 재사용 불가

### Auth
- `loginId + password` 기반 local 로그인
- access token(JWT) + refresh token(opaque random token) 발급
- Redis TTL 기반 refresh token 저장
- refresh token rotation 기반 access token 재발급
- 인증된 사용자 기준 local logout
- `/api/auth/me` 인증 사용자 정보 조회

### OIDC / SSO 확장
- local JWT 로그인 구조 유지
- Keycloak access token 수용 가능
- `ExternalIdentity`로 외부 사용자와 내부 User 를 명시적으로 연결
- 자동 회원가입 없이 연결된 사용자만 인증 성공 처리

### Audit
- role / menu / permission / user 도메인의 create / update / delete 성공 시 감사 로그 저장
- 목록 조회, 단건 조회
- `page`, `size`, `domainType`, `actionType`, `actorLoginId` 기준 검색 및 페이징 조회

## 4. 현재 구조

```text
src/main/java/com/ilbo18/authrbac
├─ domain
│  ├─ auth
│  │  ├─ controller  : login, reissue, logout, me
│  │  ├─ entity      : external identity
│  │  ├─ record      : auth request/response DTO
│  │  ├─ repository  : external identity 조회
│  │  └─ service     : local 인증, refresh token 저장/검증/삭제
│  ├─ audit          : 감사 로그 저장/조회
│  ├─ health         : 헬스 체크
│  ├─ menu           : 메뉴 CRUD, 트리 조회
│  ├─ permission     : 역할-메뉴 권한 CRUD
│  ├─ role           : 역할 CRUD
│  └─ user           : 사용자 CRUD
└─ global
   ├─ config         : Security 설정
   ├─ entity         : BaseEntity
   ├─ enumeration    : 공통 에러 코드
   ├─ exception      : 공통 예외 처리
   ├─ response       : API 응답 래퍼
   ├─ security       : local JWT 필터, OIDC 변환기, 공통 인가 필터
   └─ util           : 공통 유틸
```

각 도메인은 `controller / entity / mapper / record / repository / service` 패턴을 유지하고, 공통 응답과 예외는 `global`에서 관리합니다.

## 5. bootstrap 전략

bootstrap 공개 API는 두지 않습니다.  
초기 실행에 필요한 관리자 데이터는 `data.sql`로 시드합니다.

- 초기 역할: `ADMIN`
- 초기 계정: `admin`
- 초기 비밀번호: `admin12!@`
- 초기 메뉴: role / user / menu / permission / audit 관리 메뉴
- 초기 권한: `ADMIN` 역할에 전체 허용

## 6. menu 경로 구조

기존의 `menu.path`는 UI 이동 경로와 API 인가 경로 의미가 섞여 있었습니다.  
이를 아래처럼 분리했습니다.

- `routePath`
  - 관리자 UI 또는 메뉴 이동 경로
  - 예: `/admin/users`
- `apiPath`
  - permission 인가 기준 경로
  - 예: `/api/users`

permission 인가는 `menu.apiPath` 기준으로만 동작합니다.

## 7. local 인증 구조

### login
1. `POST /api/auth/login`
2. `loginId`, `password` 검증
3. access token(JWT) 발급
4. refresh token(opaque random token) 생성
5. refresh token은 Redis에 TTL과 함께 저장

### reissue
1. `POST /api/auth/reissue`
2. body 의 refresh token 검증
3. Redis에서 현재 사용자 최신 refresh token 인지 확인
4. 기존 refresh token 삭제
5. 새 access token + 새 refresh token 발급

### logout
1. `POST /api/auth/logout`
2. access token 으로 현재 사용자 인증
3. Redis 에서 현재 사용자의 refresh token 삭제

access token은 stateless JWT로 유지하고, 서버에서 직접 저장하거나 삭제하지 않습니다.  
즉 logout 시 바로 삭제되는 대상은 Redis 의 refresh token 뿐입니다.

access token 즉시 무효화가 필요한 구조라면 Redis blacklist 로 확장할 수 있지만, 이번 phase 에서는 범위에 포함하지 않았습니다.

## 8. Keycloak OIDC / SSO 확장 구조

이번 phase 4-2에서는 local 인증을 없애지 않고, Keycloak access token 도 동시에 받을 수 있게 확장합니다.

### Keycloak 인증 흐름
1. 클라이언트가 Keycloak 에서 access token 발급
2. `security.keycloak.enabled=true` 로 OIDC 수용 활성화
3. Spring Security resource server 가 Keycloak JWT 서명과 issuer 검증
4. `KeycloakJwtAuthenticationConverter`가 Keycloak `sub` 로 `ExternalIdentity` 조회
5. 연결된 내부 User 를 찾아 `AuthenticatedUser(userId, loginId, roleId)` 생성
6. 이후 RBAC 와 audit 흐름은 local JWT 와 동일하게 동작

이 구조의 핵심은 인증 제공자는 달라도 내부 권한 기준은 그대로 유지된다는 점입니다.

## 9. ExternalIdentity 매핑 정책

Keycloak 사용자라고 해서 내부 User 를 자동 생성하지 않습니다.  
반드시 `ExternalIdentity` 로 명시적으로 연결된 사용자만 인증 성공 처리합니다.

이 정책을 택한 이유는 사내 관리자 시스템 특성상, 외부 IdP 사용자 전체가 아니라 내부에서 허용한 사용자만 관리자 권한 체계에 연결해야 하기 때문입니다.

매핑 기준은 Keycloak `sub` 입니다.

- `preferred_username`은 바뀔 수 있습니다.
- `sub`는 Keycloak 이 보장하는 더 안정적인 사용자 식별자입니다.

예시 SQL:

```sql
insert into external_identities (
    user_id,
    provider,
    provider_user_id,
    enabled,
    deleted,
    created_at,
    updated_at,
    created_by,
    updated_by
) values (
    1,
    'KEYCLOAK',
    '2c9b8d3e-....',
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
);
```

이 phase 에서는 연결 관리 화면이나 자동 가입은 만들지 않고, 명시적 매핑 구조만 추가합니다.

## 10. refresh token 저장 전략

local 로그인에 대해서만 refresh token 을 Redis 로 관리합니다.

- `auth:refresh:{refreshToken}` -> `userId`
- `auth:refresh:user:{userId}` -> `refreshToken`

이 2-key 구조를 쓰는 이유는 다음과 같습니다.

- Redis TTL 로 만료를 단순하게 관리할 수 있습니다.
- 사용자당 최신 refresh token 1개만 유지할 수 있습니다.
- 로그인, 재발급, 로그아웃 시 토큰 교체와 삭제 흐름이 짧고 명확합니다.

Keycloak token lifecycle 은 Keycloak 이 담당하므로, 이 Redis refresh token 저장소와는 분리됩니다.

## 11. 인가 흐름

1. local JWT 또는 Keycloak JWT 로 인증
2. 두 인증 방식 모두 최종 principal 은 `AuthenticatedUser(userId, loginId, roleId)`
3. `ApiAuthorizationRule`이 현재 요청 path 와 `menu.apiPath`를 매칭
4. HTTP Method를 액션으로 변환
   - `GET -> READ`
   - `POST -> CREATE`
   - `PUT / PATCH -> UPDATE`
   - `DELETE -> DELETE`
5. `roleId + menuId` 기준 permission 조회
6. 권한이 없거나 액션 플래그가 false면 `FORBIDDEN`

아래 경로는 permission 검사 대상에서 제외합니다.

- `/api/auth/login`
- `/api/auth/reissue`
- `/api/auth/logout`
- `/api/auth/me`
- `/api/health`

중요한 점은 permission 기준이 local token 의 claim 이나 Keycloak role claim 이 아니라, 항상 내부 `roleId` 라는 점입니다.

## 12. 감사 로그 흐름

role / menu / permission / user 도메인의 create / update / delete 성공 직후 감사 로그를 저장합니다.

감사 로그에는 아래 정보가 남습니다.

- `actorUserId`
- `actorLoginId`
- `domainType`
- `actionType`
- `targetId`
- `description`
- `createdAt`

local JWT 와 Keycloak JWT 모두 내부 `AuthenticatedUser` 로 정리되므로, audit 구조는 그대로 재사용됩니다.

## 13. audit 조회 범위

현재 audit 조회는 아래 수준까지 지원합니다.

- 페이징: `page`, `size`
- 검색: `domainType`, `actionType`, `actorLoginId`
- 정렬: `createdAt desc`, `id desc`

현재 조건 수가 많지 않아서 Spring Data JPA `ExampleMatcher + PageRequest` 구조를 유지합니다.

## 14. 실행 방법

### Redis 실행

```bash
docker run --name auth-rbac-redis -p 6379:6379 -d redis:7
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### 컴파일

```bash
./gradlew compileJava
```

### 테스트 실행

```bash
./gradlew test
```

### H2 콘솔

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:authdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`

### 초기 로그인

- loginId: `admin`
- password: `admin12!@`

### Keycloak 연동 활성화 예시

기본값은 local 실행 기준으로 비활성화입니다.

```properties
security.keycloak.enabled=true
security.keycloak.issuer-uri=http://localhost:8081/realms/auth-rbac-admin
```

## 15. 테스트 요약

현재 테스트는 아래 범위를 검증합니다.

- AuthControllerTest
  - 로그인 성공 / 실패
  - refresh token 재발급 / rotation / 로그아웃
  - `/api/auth/me` 인증 성공 / 실패
  - JWT 이후 실제 permission 인가 흐름
- UserServiceTest
  - 생성, 중복 loginId, invalid role, 수정, 삭제 후 조회 실패
- PermissionServiceTest
  - 생성, 중복 조합, invalid role/menu, 액션 검증, 수정, 삭제 후 재생성
- RoleServiceTest / MenuServiceTest
  - 기본 CRUD 흐름
  - 메뉴 트리 조회
- AuditControllerTest
  - 감사 로그 적재
  - 목록 / 단건 조회
  - 검색 / 페이징 조회
  - 무토큰 접근 실패

현재 `application.properties`의 Redis 정보, JWT secret, Keycloak issuer 는 로컬 실행 예시 기준입니다. 운영 환경에서는 profile 또는 환경변수로 분리하는 것을 전제로 합니다.

## 16. 이번 phase에서 의도적으로 제외한 것

- 네이버 / 구글 소셜 로그인
- 자동 회원가입
- 다중 realm / multi-tenant
- access token blacklist
- Keycloak 인프라 자동 구축 스크립트
- 조직 / 부서 동기화
- 계정 연결 관리 화면
- Keycloak logout 연동
