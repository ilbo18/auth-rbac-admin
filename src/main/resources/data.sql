-- 로컬 데모와 포트폴리오 실행을 위한 초기 시드다.
-- bootstrap 공개 API 없이도 로그인, 인가, 감사 로그 흐름을 바로 확인할 수 있게 최소 관리자 데이터를 넣는다.

insert into roles (
    id,
    code,
    name,
    description,
    enabled,
    deleted,
    created_at,
    updated_at,
    created_by,
    updated_by
) values (
    1,
    'ADMIN',
    '관리자',
    '초기 관리자 역할',
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
);

-- route_path 는 UI 또는 메뉴 이동 경로다.
-- api_path 는 permission 인가 기준 경로다.
insert into menus (
    id,
    name,
    route_path,
    api_path,
    parent_id,
    sort_order,
    enabled,
    deleted,
    created_at,
    updated_at,
    created_by,
    updated_by
) values
(
    1,
    '역할 관리',
    '/admin/roles',
    '/api/roles',
    null,
    1,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    2,
    '사용자 관리',
    '/admin/users',
    '/api/users',
    null,
    2,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    3,
    '메뉴 관리',
    '/admin/menus',
    '/api/menus',
    null,
    3,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    4,
    '권한 관리',
    '/admin/permissions',
    '/api/permissions',
    null,
    4,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    5,
    '감사 로그',
    '/admin/audits',
    '/api/audits',
    null,
    5,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
);

insert into users (
    id,
    login_id,
    password,
    name,
    role_id,
    enabled,
    deleted,
    created_at,
    updated_at,
    created_by,
    updated_by
) values (
    1,
    'admin',
    '$2a$10$.lJgNPlEaF/vIXa/PFZl4.Iz3KKuWK2pmnCf1UPG5/id4GT7t/flW',
    '초기 관리자',
    1,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
);

-- ADMIN 역할은 데모 실행 직후 주요 관리 API를 모두 확인할 수 있도록 전체 권한을 가진다.
insert into permissions (
    id,
    role_id,
    menu_id,
    can_read,
    can_create,
    can_update,
    can_delete,
    enabled,
    deleted,
    created_at,
    updated_at,
    created_by,
    updated_by
) values
(
    1,
    1,
    1,
    true,
    true,
    true,
    true,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    2,
    1,
    2,
    true,
    true,
    true,
    true,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    3,
    1,
    3,
    true,
    true,
    true,
    true,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    4,
    1,
    4,
    true,
    true,
    true,
    true,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
),
(
    5,
    1,
    5,
    true,
    true,
    true,
    true,
    true,
    false,
    current_timestamp,
    current_timestamp,
    'system',
    'system'
);
