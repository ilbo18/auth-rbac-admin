package com.ilbo18.authrbac.domain.permission.controller;

import com.ilbo18.authrbac.domain.permission.record.PermissionRecord;
import com.ilbo18.authrbac.domain.permission.service.PermissionService;
import com.ilbo18.authrbac.global.response.ApiResponseRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 권한 관리 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    /** 권한 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseRecord<String>> createPermission(@Valid @RequestBody PermissionRecord.Create req) {
        permissionService.createPermission(req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 권한 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseRecord<List<PermissionRecord.Response>>> getPermissions() {
        return ResponseEntity.ok(ApiResponseRecord.success(permissionService.getPermissions()));
    }

    /** 권한 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<PermissionRecord.Response>> getPermission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseRecord.success(permissionService.getPermission(id)));
    }

    /** 권한 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionRecord.Update req) {
        permissionService.updatePermission(id, req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 권한 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }
}
