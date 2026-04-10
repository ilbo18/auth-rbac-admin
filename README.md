# auth-rbac-admin

`auth-rbac-admin`은 Spring Boot 기반 관리자 백엔드 포트폴리오 프로젝트입니다.  
JWT 인증, RBAC, 메뉴 단위 권한 관리, 감사 로그를 한 흐름으로 묶어 관리자 시스템에서 자주 필요한 인증/인가 문제를 직접 구현하고 검증하는 데 목적이 있습니다.

## 1. 프로젝트 소개

이 프로젝트는 단순 CRUD 예제가 아니라 인증 이후 인가와 감사 추적까지 이어지는 관리자 백엔드의 핵심 흐름을 구현하는 데 초점을 맞췄습니다.

- 관리자 계정 로그인과 JWT 발급
- Role, Menu, Permission, User 관리
- 요청 경로와 HTTP Method 기반의 실제 API 인가
- 관리자 작업 이력을 남기는 Audit 로그
- 테스트 코드까지 포함한 실무형 백엔드 포트폴리오

## 2. 기술 스택

- Java 21
- Spring Boot 3
- Gradle
- Spring Security
- Spring Data JPA
- H2
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
- `loginId + password` 로그인
- JWT access token 발급
- `/api/auth/me` 인증 사용자 정보 조회

### Audit
- role / menu / permission / user 도메인의 create / update / delete 성공 시 감사 로그 저장
- 목록 조회, 단건 조회
- `page`, `size`, `domainType`, `actionType`, `actorLoginId` 기준 검색 및 페이징 조회

## 4. 현재 구조

```text
src/main/java/com/ilbo18/authrbac
├─ domain
│  ├─ auth        : 로그인, JWT 발급, 인증 사용자 조회
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

phase 3 이후에는 bootstrap 공개 API를 두지 않습니다.  
초기 실행에 필요한 관리자 데이터는 `data.sql`로 시드합니다.

- 초기 역할: `ADMIN`
- 초기 계정: `admin`
- 초기 비밀번호: `Admin123!`
- 초기 메뉴: role / user / menu / permission / audit 관리 메뉴
- 초기 권한: `ADMIN` 역할에 전체 허용

이 방식은 공개 bootstrap API를 남기는 것보다 설명이 단순하고, 보안 정책도 일관되게 유지할 수 있습니다.  
포트폴리오 기준에서는 “실행 직후 바로 로그인과 인가 흐름을 확인할 수 있는가”가 더 중요하다고 판단했습니다.

## 6. menu 경로 구조

기존의 `menu.path`는 UI 이동 경로와 API 인가 경로 의미가 섞여 있었습니다.  
이를 아래처럼 분리했습니다.

- `routePath`
  - 관리자 UI 또는 메뉴 이동 경로
  - 예: `/admin/users`
- `apiPath`
  - permission 인가 기준 경로
  - 예: `/api/users`

이후 permission 인가는 `menu.apiPath` 기준으로만 동작합니다.  
UI 라우팅과 API 보호 기준을 분리해 메뉴 의미를 더 명확하게 유지합니다.

## 7. 인증 / 인가 흐름

### 인증
1. `POST /api/auth/login` 요청
2. `loginId`, `password` 검증
3. JWT access token 발급
4. 이후 요청은 `Authorization: Bearer {token}` 헤더 사용
5. JWT 필터가 토큰을 검증하고 `SecurityContext`에 `AuthenticatedUser(userId, loginId, roleId)`를 저장

### 인가
1. 인증된 요청의 `roleId`를 확인
2. 현재 요청 path 와 `menu.apiPath`를 매칭
3. HTTP Method를 액션으로 변환
   - `GET -> READ`
   - `POST -> CREATE`
   - `PUT / PATCH -> UPDATE`
   - `DELETE -> DELETE`
4. `roleId + menuId` 기준 permission 조회
5. 권한이 없거나 액션 플래그가 false면 `FORBIDDEN`

인가 규칙은 JWT 필터 안에 모두 넣지 않고, `ApiAuthorizationRule`에서 판단하도록 분리해 책임을 명확히 유지합니다.

## 8. 감사 로그 흐름

role / menu / permission / user 도메인의 create / update / delete 성공 직후 감사 로그를 저장합니다.

감사 로그에는 아래 정보가 남습니다.

- `actorUserId` : 작업 수행 사용자 ID
- `actorLoginId` : 작업 수행 로그인 ID
- `domainType` : ROLE / MENU / PERMISSION / USER
- `actionType` : CREATE / UPDATE / DELETE
- `targetId` : 작업 대상 ID
- `description` : 예) `ROLE 생성`
- `createdAt` : 로그 생성 시각

인증 정보가 없는 경우에는 안전하게 `actorLoginId = system`으로 저장합니다.

## 9. audit 조회 범위

현재 audit 조회는 아래 수준까지 지원합니다.

- 페이징: `page`, `size`
- 검색: `domainType`, `actionType`, `actorLoginId`
- 정렬: `createdAt desc`, `id desc`

현재 조건 수가 많지 않아서 Spring Data JPA `ExampleMatcher + PageRequest` 조합을 유지합니다.  
검색 조건이 더 복잡해지거나 조합식 필터가 늘어나는 시점에 QueryDSL 또는 JPQL 도입을 검토할 수 있습니다.

## 10. 실행 방법

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

## 11. 테스트 요약

현재 테스트는 아래 범위를 검증합니다.

- AuthControllerTest
  - 로그인 성공 / 실패
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

## 12. 향후 개선 로드맵

### phase 4 또는 최종 고도화 단계
- refresh token / logout
- permission 세분화 고도화
- audit 검색 조건 확장 및 다운로드
- menu 트리 고도화
- OAuth2 로그인 연동
- OIDC 기반 사용자 인증 위임
- 사내 SSO 연동 확장

현재 구조는 `JWT + Spring Security + AuthenticatedUser` 기반으로 인증 문맥을 분리하고 있어, 향후 OAuth2 / OIDC / SSO로 확장할 수 있는 기반을 갖추고 있습니다.
