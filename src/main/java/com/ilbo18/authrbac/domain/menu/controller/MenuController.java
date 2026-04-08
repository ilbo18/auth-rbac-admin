package com.ilbo18.authrbac.domain.menu.controller;

import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import com.ilbo18.authrbac.domain.menu.service.MenuService;
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
 * 메뉴 관리 API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    /** 메뉴 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseRecord<String>> createMenu(@Valid @RequestBody MenuRecord.Create req) {
        menuService.createMenu(req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 메뉴 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseRecord<List<MenuRecord.Response>>> getMenus() {
        return ResponseEntity.ok(ApiResponseRecord.success(menuService.getMenus()));
    }

    /** 메뉴 트리 조회 */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponseRecord<List<MenuRecord.TreeResponse>>> getMenuTree() {
        return ResponseEntity.ok(ApiResponseRecord.success(menuService.getMenuTree()));
    }

    /** 메뉴 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<MenuRecord.Response>> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseRecord.success(menuService.getMenu(id)));
    }

    /** 메뉴 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuRecord.Update req) {
        menuService.updateMenu(id, req);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }

    /** 메뉴 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseRecord<String>> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);

        return ResponseEntity.ok(ApiResponseRecord.success());
    }
}
