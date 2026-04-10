# auth-rbac-admin

`auth-rbac-admin`은 Spring Boot 기반 관리자 백엔드 포트폴리오 프로젝트입니다.  
JWT 인증, RBAC, 메뉴 단위 권한 관리, 감사 로그를 한 흐름으로 묶어 관리자 시스템에서 자주 필요한 인증/인가 문제를 직접 구현하고 검증하는 데 목적이 있습니다.

## 1. 프로젝트 소개

이 프로젝트는 단순 CRUD 예제가 아니라 인증 이후 인가와 감사 추적까지 이어지는 관리자 백엔드의 핵심 흐름을 구현하는 데 초점을 맞췄습니다.

- 관리자 계정 로그인과 JWT 발급
- Role, Menu, Permission, User 관리
- 요청 경로와 HTTP Method 기반의 실제 API 인가
- 관리자 작업 이력을 남기는 Audit 로그
- Redis 기반 refresh token / logout / token reissue
- 테스트 코드까지 포함한 실무형 백엔드 포트폴리오

## 2. 기술 스택

- Java 21
- Spring Boot 3
- Gradle
- Spring Security
- Spring Data JPA
- Spring Data Redis
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
- `loginId + password` local 로그인
- access token + refresh token 발급
- refresh token rotation 기반 access token 재발급
- 인증된 사용자 기준 logout
- `/api/auth/me` 인증 사용자 정보 조회

### Audit
- role / menu / permission / user 도메인의 create / update / delete 성공 시 감사 로그 저장
- 목록 조회, 단건 조회
- `page`, `size`, `domainType`, `actionType`, `actorLoginId` 기준 검색 및 페이징 조회

## 4. 현재 구조

```text
src/main/java/com/ilbo18/authrbac
├─ domain
│  ├─ auth        : login, reissue, logout, me
│  ├─ audit       : 감사 로그 저장/조회
│  ├─ health      : 헬스 체크
│  ├─ menu        : 메뉴 CRUD, 트리 조회
│  ├─ permission  : 역할-메뉴 권한 CRUD
│  ├─ role        : 역할 CRUD
│  └─ user        : 사용자 CRUD
└─ global
   ├─ config      : Security 설정
   ├─ entity      : BaseEntity
   ├─ enumeration : 공통 에러 코드
   ├─ exception   : 공통 예외 처리
   ├─ response    : API 응답 래퍼
   ├─ security    : JWT 필터, 토큰 처리, 인가 규칙
   └─ util        : 공통 유틸
```

각 도메인은 `controller / entity / mapper / record / repository / service` 패턴을 유지하고, 공통 응답과 예외는 `global`에서 관리합니다.

## 5. bootstrap 전략

bootstrap 공개 API는 두지 않습니다.  
초기 실행에 필요한 관리자 데이터는 `data.sql`로 시드합니다.

- 초기 역할: `ADMIN`
- 초기 계정: `admin`
- 초기 비밀번호: `Admin123!`
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

## 7. 인증 구조

### login
1. `POST /api/auth/login`
2. `loginId`, `password` 검증
3. access token 발급
4. opaque refresh token 생성
5. refresh token은 Redis에 TTL과 함께 저장

### reissue
1. `POST /api/auth/reissue`
2. body 의 refresh token만으로 사용자 세션을 조회
3. Redis에서 refresh token과 사용자 최신 토큰 여부 확인
4. 기존 refresh token 삭제
5. 새 access token + 새 refresh token 발급

### logout
1. `POST /api/auth/logout`
2. access token으로 현재 사용자 인증
3. Redis에서 현재 사용자 refresh token 삭제

access token은 stateless JWT로 유지하고, refresh token만 Redis에 저장해 재발급과 로그아웃 흐름을 분리했습니다.
서버가 직접 삭제하는 대상은 Redis의 refresh token뿐이고, 이미 발급된 access token은 저장소에서 제거하지 않습니다.
access token 즉시 무효화는 이번 phase 4-1 범위가 아니며, 필요하면 blacklist 방식으로 확장할 수 있습니다.

## 8. refresh token 저장 전략

phase 4-1에서는 DB 테이블 대신 Redis를 사용합니다.

- `auth:refresh:{refreshToken}` -> `userId`
- `auth:refresh:user:{userId}` -> `refreshToken`

이 구조를 쓰는 이유는 두 가지입니다.

- TTL로 만료를 Redis에 맡길 수 있습니다.
- 사용자당 최신 refresh token 1개만 유지해 로그인 교체, rotation, logout을 짧게 설명할 수 있습니다.
- reissue는 refresh token만으로 동작하고, logout은 access token으로 인증된 현재 사용자 기준으로만 동작합니다.

## 9. 인가 흐름

1. access token으로 인증
2. JWT 필터가 `SecurityContext`에 `AuthenticatedUser(userId, loginId, roleId)`를 저장
3. `ApiAuthorizationRule`이 현재 요청 path 와 `menu.apiPath`를 매칭
4. HTTP Method를 액션으로 변환
   - `GET -> READ`
   - `POST -> CREATE`
   - `PUT / PATCH -> UPDATE`
   - `DELETE -> DELETE`
5. `roleId + menuId` 기준 permission 조회
6. 권한이 없거나 액션 플래그가 false면 `FORBIDDEN`

`/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout`, `/api/auth/me`, `/api/health`는 permission 검사 대상에서 제외합니다.  
이 중 `/api/auth/logout`은 permission 제외일 뿐이고, access token 인증은 여전히 필요합니다.

## 10. 감사 로그 흐름

role / menu / permission / user 도메인의 create / update / delete 성공 직후 감사 로그를 저장합니다.

감사 로그에는 아래 정보가 남습니다.

- `actorUserId`
- `actorLoginId`
- `domainType`
- `actionType`
- `targetId`
- `description`
- `createdAt`

인증 정보가 없는 경우에는 `actorLoginId = system`으로 저장합니다.

## 11. audit 조회 범위

현재 audit 조회는 아래 수준까지 지원합니다.

- 페이징: `page`, `size`
- 검색: `domainType`, `actionType`, `actorLoginId`
- 정렬: `createdAt desc`, `id desc`

현재 조건 수가 많지 않아서 Spring Data JPA `ExampleMatcher + PageRequest` 구조를 유지합니다.

## 12. 실행 방법

### Redis 실행

로컬에서는 Redis가 먼저 실행되어 있어야 합니다.

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

Windows 환경에서 한글 사용자 홈 경로로 Gradle 테스트 워커 이슈가 있다면 `GRADLE_USER_HOME`을 짧은 경로로 지정해 실행하면 됩니다.

### H2 콘솔

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:authdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`

### 초기 로그인

초기 데이터는 로컬 데모와 포트폴리오 실행을 위한 시드입니다.

- loginId: `admin`
- password: `Admin123!`

### http 파일로 수동 검증

프로젝트 루트의 `http` 디렉터리에서 아래 파일로 기능 흐름을 확인할 수 있습니다.

- `auth.http`
- `role.http`
- `menu.http`
- `permission.http`
- `user.http`
- `audit.http`
- `health.http`

## 13. 테스트 요약

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

## 14. phase 4-1 범위

이번 단계에서 실제 반영하는 것은 여기까지입니다.

- local JWT 유지
- Redis refresh token 저장
- refresh token rotation
- logout
- access token reissue

아래는 이번 단계에서 실제 구현하지 않습니다.

- Keycloak 연동
- OIDC / SSO 실제 코드
- 외부 IdP 사용자 매핑
- access token 블랙리스트
- 다중 디바이스 세션 관리

## 15. phase 4-2 방향

다음 단계에서는 Keycloak 기반 사내망 SSO 확장으로 이어질 수 있습니다.

- local JWT 인증 구조는 유지
- 외부 IdP 인증 결과를 내부 사용자와 연결
- 최종적으로 permission 과 audit 구조는 재사용

즉, 이번 phase 4-1은 직접 구현한 로컬 인증을 먼저 완성하고, phase 4-2에서 Keycloak 기준 확장 포인트를 얹는 순서로 가져가는 것이 목적입니다.
