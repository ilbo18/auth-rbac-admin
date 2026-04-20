# auth-rbac-admin

`auth-rbac-admin`은 Spring Boot 기반 관리자 백엔드 포트폴리오 프로젝트입니다.  
main 브랜치 하나에서 인증 모드만 전환하고, 그 이후의 RBAC와 audit 구조는 공통으로 재사용하는 형태로 마무리했습니다.

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
- Keycloak
- JUnit5
- MockMvc

## 3. 핵심 구조

이 프로젝트는 인증만 모드별로 다르고, 인증 이후 구조는 공통입니다.

- 인증
  - `auth.mode=local`
    - standalone JWT 로그인
    - Redis refresh token 사용
  - `auth.mode=keycloak`
    - Keycloak access token 기반 SSO
    - Resource Server 방식 인증
- 공통 principal
  - 두 모드 모두 최종 principal 은 `AuthenticatedUser(userId, loginId, roleId)`
- 공통 인가
  - `ApiAuthorizationFilter + ApiAuthorizationRule`
  - `menu.apiPath + HTTP method + roleId + permission`
- 공통 감사 로그
  - 인증 이후 흐름이 같기 때문에 audit 구조도 그대로 재사용

## 4. 인증 모드

### local mode

빠르게 프로젝트를 확인하거나, 외부 IdP 없이 standalone 으로 인증/인가 흐름을 검증할 때 쓰는 모드입니다.  
최종 데모 권장 모드는 아니고, 저장소를 clone 한 뒤 가장 빨리 동작을 확인하는 용도에 가깝습니다.

- `loginId + password` local 로그인
- access token(JWT) 발급
- refresh token(opaque random token) 발급
- Redis TTL 기반 refresh token 저장
- reissue / logout 지원
- `/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout` 사용

### keycloak mode

최종 권장 데모 모드입니다.  
사내망 SSO 시나리오를 설명하기 좋은 구조로, Keycloak access token 을 받아도 내부 RBAC 기준은 그대로 유지합니다.

- Keycloak OIDC access token 수용
- Spring Security Resource Server 로 서명/issuer 검증
- `ExternalIdentity -> User -> roleId -> Permission` 흐름으로 내부 권한에 연결
- 외부 role claim 직접 사용 금지
- 자동 회원가입 금지
- `/api/auth/me` 와 보호된 API를 Keycloak access token 기준으로 호출

## 5. 권한 구조

### menu 경로 구조

- `routePath`
  - 관리자 UI 또는 메뉴 이동 경로
  - 예: `/admin/users`
- `apiPath`
  - permission 인가 기준 경로
  - 예: `/api/users`

permission 인가는 `menu.apiPath` 기준으로만 동작합니다.

### 공통 인가 흐름

1. local JWT 또는 Keycloak JWT 로 인증
2. 최종 principal 을 `AuthenticatedUser(userId, loginId, roleId)` 로 통일
3. `ApiAuthorizationFilter` 가 공통 permission 검사 시작
4. `ApiAuthorizationRule` 이 현재 요청 path 와 `menu.apiPath` 를 매칭
5. HTTP Method 를 액션으로 변환
   - `GET -> READ`
   - `POST -> CREATE`
   - `PUT / PATCH -> UPDATE`
   - `DELETE -> DELETE`
6. `roleId + menuId` 기준 permission 조회
7. 권한이 없거나 액션 플래그가 false 면 `FORBIDDEN`

중요한 점은 인증 제공자가 달라도 내부 RBAC 기준은 항상 내부 `roleId` 라는 점입니다.

## 6. ExternalIdentity 정책

Keycloak 사용자는 내부 User 에 자동으로 가입되지 않습니다.  
반드시 `ExternalIdentity` 로 명시적으로 연결된 사용자만 인증 성공 처리합니다.

이 정책을 유지한 이유는 사내 관리자 시스템 특성상, 외부 IdP 사용자 전체가 아니라 내부에서 허용한 관리자만 권한 체계에 연결해야 하기 때문입니다.

매핑 기준은 Keycloak `sub` 입니다.

- `sub` -> `ExternalIdentity.providerUserId`
- `provider=KEYCLOAK`
- 연결된 내부 `userId` 조회
- 내부 `User.enabled` 확인
- 내부 `roleId` 기준 RBAC 적용

즉 외부 IdP 의 사용자 식별자는 인증용이고, 실제 관리자 권한 기준은 내부 데이터가 담당합니다.

## 7. local mode 인증 흐름

아래 API 는 local mode 전용 API 입니다.  
keycloak mode 에서는 권장 사용 대상이 아니고, 실제 데모도 Keycloak access token 기준으로 진행하는 편이 맞습니다.

### login
1. `POST /api/auth/login`
2. `loginId`, `password` 검증
3. access token(JWT) 발급
4. refresh token(opaque random token) 생성
5. refresh token 은 Redis 에 TTL 과 함께 저장

### reissue
1. `POST /api/auth/reissue`
2. body 의 refresh token 검증
3. Redis 에서 현재 사용자 최신 refresh token 인지 확인
4. 기존 refresh token 삭제
5. 새 access token + 새 refresh token 발급

### logout
1. `POST /api/auth/logout`
2. access token 으로 현재 사용자 인증
3. Redis 에서 현재 사용자의 refresh token 삭제

access token 은 stateless JWT 로 유지하고, 서버에서 직접 저장하거나 삭제하지 않습니다.  
즉 logout 시 바로 삭제되는 대상은 Redis 의 refresh token 뿐입니다.

access token 즉시 무효화가 필요하면 blacklist 로 확장할 수 있지만, 이번 프로젝트 범위에는 포함하지 않았습니다.

## 8. keycloak mode 인증 흐름

1. 클라이언트가 Keycloak 에서 access token 발급
2. `auth.mode=keycloak` 로 실행
3. Resource Server 가 Keycloak JWT 서명과 issuer 검증
4. `KeycloakJwtAuthenticationConverter` 가 Keycloak `sub` 로 `ExternalIdentity` 조회
5. 연결된 내부 `User` 와 `roleId` 를 찾은 뒤 `AuthenticatedUser` 생성
6. 이후 RBAC 와 audit 흐름은 local mode 와 동일하게 동작

이 구조의 핵심은 인증 공급자가 바뀌어도 인가와 감사 로그 구조는 바뀌지 않는다는 점입니다.

## 9. 감사 로그

role / menu / permission / user 도메인의 create / update / delete 성공 직후 감사 로그를 저장합니다.

감사 로그에는 아래 정보가 남습니다.

- `actorUserId`
- `actorLoginId`
- `domainType`
- `actionType`
- `targetId`
- `description`
- `createdAt`

local JWT 와 Keycloak JWT 모두 최종 principal 이 `AuthenticatedUser` 이므로 audit 구조는 그대로 재사용됩니다.

## 10. 실행 방법

### docker compose

1. 저장소 루트에서 먼저 아래 명령으로 Redis 와 Keycloak 을 띄웁니다.

```bash
docker compose up -d
```

2. 그 다음 원하는 인증 모드로 애플리케이션을 실행합니다.

기본 포트는 아래와 같습니다.

- Redis: `6379`
- Keycloak: `8081`

Keycloak container 는 dev mode 로만 띄웁니다.  
realm, client, user, external identity 매핑 데이터는 이 프로젝트에서 자동 생성하지 않습니다.

### local mode 실행

빠른 확인용 standalone 모드입니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

초기 로그인:

- loginId: `admin`
- password: `admin12!@`

local mode 에서 직접 확인하는 API:

- `POST /api/auth/login`
- `POST /api/auth/reissue`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### keycloak mode 실행

최종 권장 데모 모드입니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=keycloak'
```

이 모드에서는 local login / reissue / logout 흐름을 데모 기준으로 쓰지 않고, Keycloak access token 으로 인증합니다.

### Keycloak 수동 초기 세팅

clone 직후 사용자가 직접 해야 하는 작업은 아래까지입니다.

1. `http://localhost:8081` 에 접속해서 `admin / admin` 으로 로그인합니다.
2. realm `auth-rbac-admin` 을 생성합니다.
3. client 를 하나 생성합니다.
   빠른 API 데모 기준으로는 `auth-rbac-admin` 같은 public client 를 만들고, 로그인 토큰 발급이 가능하도록 설정하면 충분합니다.
4. 테스트용 사용자를 하나 생성하고 비밀번호를 설정합니다.
5. 방금 만든 사용자로 access token 을 발급받고, token payload 의 `sub` 값을 확인합니다.
6. 애플리케이션 DB 의 `external_identities` 테이블에 아래처럼 매핑 데이터를 넣습니다.

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
    'keycloak-user-sub',
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
);
```

7. 이후 Keycloak access token 으로 `GET /api/auth/me` 와 보호된 API 를 호출합니다.

이 프로젝트는 Keycloak 사용자 자동 가입을 하지 않습니다.  
즉 `ExternalIdentity` 매핑을 넣기 전까지는 Keycloak access token 이 유효해도 내부 인증은 성공하지 않습니다.

## 11. 설정 파일

- `application.properties`
  - 공통 설정
  - 코드 기본값은 `auth.mode=local`
- `application-local.properties`
  - local mode 전용 설정
- `application-keycloak.properties`
  - keycloak mode 전용 설정

profile 또는 설정만 바꾸면 두 모드를 같은 main 브랜치에서 모두 실행할 수 있습니다.

정리하면 clone 후 바로 되는 범위는 local mode 까지이고, keycloak mode 는 아래 수동 준비가 추가로 필요합니다.

- Keycloak realm 생성
- Keycloak client 생성
- Keycloak test user 생성
- access token 발급 후 `sub` 확인
- `external_identities` 매핑 row 추가

## 12. bootstrap 전략

bootstrap 공개 API 는 두지 않습니다.  
초기 실행에 필요한 관리자 데이터는 `data.sql` 로 시드합니다.

- 초기 역할: `ADMIN`
- 초기 계정: `admin`
- 초기 비밀번호: `admin12!@`
- 초기 메뉴: role / user / menu / permission / audit 관리 메뉴
- 초기 권한: `ADMIN` 역할에 전체 허용

## 13. 테스트

현재 테스트는 아래 범위를 검증합니다.

- local JWT 로그인 / 재발급 / 로그아웃
- local JWT 이후 permission 인가 흐름
- audit 적재 / 조회
- Keycloak JWT -> ExternalIdentity -> User 변환 흐름

이번 단계에서는 과한 통합 테스트 대신, Keycloak 변환 로직만 단위 테스트로 추가했습니다.

## 14. 이번 프로젝트에서 의도적으로 제외한 것

- 네이버 / 구글 소셜 로그인
- 자동 회원가입
- 다중 realm / multi-tenant
- access token blacklist
- Keycloak 인프라 자동 구축
- 조직 / 부서 동기화
- 계정 연결 관리 화면
- Keycloak logout 연동

이 프로젝트의 목표는 인증 공급자별 로그인 화면을 만드는 것이 아니라, 인증 결과를 내부 RBAC 구조에 안전하게 연결하는 것입니다.
