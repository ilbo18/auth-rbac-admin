package com.ilbo18.authrbac.domain.menu.service;

import com.ilbo18.authrbac.domain.menu.record.MenuRecord;

import java.util.List;

public interface MenuService {

    /** 메뉴 생성 */
    void createMenu(MenuRecord.Create req);

    /** 메뉴 목록 조회 */
    List<MenuRecord.Response> getMenus();

    /** 메뉴 단건 조회 */
    MenuRecord.Response getMenu(Long id);

    /** 메뉴 수정 */
    void updateMenu(Long id, MenuRecord.Update req);

    /** 메뉴 삭제 */
    void deleteMenu(Long id);
}
