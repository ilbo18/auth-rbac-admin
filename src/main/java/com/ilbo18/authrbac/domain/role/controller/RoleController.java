package com.ilbo18.authrbac.domain.role.controller;

import com.ilbo18.authrbac.domain.role.record.RoleRecord;
import com.ilbo18.authrbac.domain.role.service.RoleService;
import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 관리 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    /** 역할 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseRecord<String>> createRole(@Valid @RequestBody RoleRecord.Create req) {
        roleService.createRole(req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }
}
