package com.ilbo18.authrbac.domain.user.controller;

import com.ilbo18.authrbac.domain.user.record.UserRecord;
import com.ilbo18.authrbac.domain.user.service.UserService;
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
 * 사용자 관리 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** 사용자 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseRecord<String>> createUser(@Valid @RequestBody UserRecord.Create req) {
        userService.createUser(req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 사용자 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseRecord<List<UserRecord.Response>>> getUsers() {
        return ResponseEntity.ok(ApiResponseRecord.success(userService.getUsers()));
    }

    /** 사용자 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<UserRecord.Response>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseRecord.success(userService.getUser(id)));
    }

    /** 사용자 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> updateUser(@PathVariable Long id, @Valid @RequestBody UserRecord.Update req) {
        userService.updateUser(id, req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 사용자 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }
}
