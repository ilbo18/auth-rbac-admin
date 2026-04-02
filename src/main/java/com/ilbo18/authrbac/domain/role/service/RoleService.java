package com.ilbo18.authrbac.domain.role.service;

import com.ilbo18.authrbac.domain.role.record.RoleRecord;

import java.util.List;

public interface RoleService {

    /** 역할 생성 */
    void createRole(RoleRecord.Create req);

    /** 역할 목록 조회 */
    List<RoleRecord.Response> getRoles();

    /** 역할 단건 조회 */
    RoleRecord.Response getRole(Long id);

    /** 역할 수정 */
    void updateRole(Long id, RoleRecord.Update req);

    /** 역할 삭제 */
    void deleteRole(Long id);
}
