package com.ilbo18.authrbac.domain.role.service;

import com.ilbo18.authrbac.domain.role.record.RoleRecord;

public interface RoleService {

    void createRole(RoleRecord.Create req);
}
