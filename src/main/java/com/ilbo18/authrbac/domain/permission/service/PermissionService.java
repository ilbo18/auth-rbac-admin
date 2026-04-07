package com.ilbo18.authrbac.domain.permission.service;

import com.ilbo18.authrbac.domain.permission.record.PermissionRecord;

import java.util.List;

public interface PermissionService {

    /** 권한 생성 */
    void createPermission(PermissionRecord.Create req);

    /** 권한 목록 조회 */
    List<PermissionRecord.Response> getPermissions();

    /** 권한 단건 조회 */
    PermissionRecord.Response getPermission(Long id);

    /** 권한 수정 */
    void updatePermission(Long id, PermissionRecord.Update req);

    /** 권한 삭제 */
    void deletePermission(Long id);
}
