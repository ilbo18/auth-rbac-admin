package com.ilbo18.authrbac.domain.role.controller;

import com.ilbo18.authrbac.domain.role.record.RoleRecord;
import com.ilbo18.authrbac.domain.role.service.RoleService;
import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /** 역할 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseRecord<List<RoleRecord.Response>>> getRoles() {
        return ResponseEntity.ok(ApiResponseRecord.success(roleService.getRoles()));
    }

    /**
     * 역할 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<RoleRecord.Response>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseRecord.success(roleService.getRole(id)));
    }

    /** 역할 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRecord.Update req) {
        roleService.updateRole(id, req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 역할 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }
}
