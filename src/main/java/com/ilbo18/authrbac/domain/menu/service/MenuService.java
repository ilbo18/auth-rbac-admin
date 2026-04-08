package com.ilbo18.authrbac.domain.menu.service;

import com.ilbo18.authrbac.domain.menu.record.MenuRecord;

import java.util.List;

public interface MenuService {

    void createMenu(MenuRecord.Create req);

    List<MenuRecord.Response> getMenus();

    List<MenuRecord.TreeResponse> getMenuTree();

    MenuRecord.Response getMenu(Long id);

    void updateMenu(Long id, MenuRecord.Update req);

    void deleteMenu(Long id);
}
