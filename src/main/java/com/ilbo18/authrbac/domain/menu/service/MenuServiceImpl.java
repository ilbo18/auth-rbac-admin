package com.ilbo18.authrbac.domain.menu.service;

import com.ilbo18.authrbac.domain.audit.enumeration.AuditActionType;
import com.ilbo18.authrbac.domain.audit.enumeration.AuditDomainType;
import com.ilbo18.authrbac.domain.audit.service.AuditService;
import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.mapper.MenuMapper;
import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import com.ilbo18.authrbac.domain.menu.repository.MenuRepository;
import com.ilbo18.authrbac.global.enumeration.AuthErrorCode;
import com.ilbo18.authrbac.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 메뉴 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    private static final Comparator<Menu> MENU_TREE_COMPARATOR = Comparator.comparing(Menu::getSortOrder)
                                                                           .thenComparing(Menu::getId);

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public void createMenu(MenuRecord.Create req) {
        validateMenuPaths(req.routePath(), req.apiPath(), null);
        validateParentMenu(req.parentId(), null);

        Menu menu = menuMapper.toEntity(req);

        menuRepository.save(menu);
        auditService.createAudit(AuditDomainType.MENU, AuditActionType.CREATE, menu.getId(), "MENU 생성");
    }

    @Override
    public List<MenuRecord.Response> getMenus() {
        return menuRepository.findAllByDeletedFalse()
                             .stream()
                             .sorted(Comparator.comparingLong(Menu::getId))
                             .map(menuMapper::toResponse)
                             .toList();
    }

    @Override
    public List<MenuRecord.TreeResponse> getMenuTree() {
        List<Menu> menus = menuRepository.findAllByDeletedFalse()
                                         .stream()
                                         .sorted(MENU_TREE_COMPARATOR)
                                         .toList();

        return buildTree(null, menus);
    }

    @Override
    public MenuRecord.Response getMenu(Long id) {
        Menu menu = Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.MENU_NOT_FOUND));

        return menuMapper.toResponse(menu);
    }

    @Override
    @Transactional
    public void updateMenu(Long id, MenuRecord.Update req) {
        Menu menu = Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.MENU_NOT_FOUND));

        validateMenuPaths(req.routePath(), req.apiPath(), id);
        validateParentMenu(req.parentId(), id);

        menu.update(req.name(), req.routePath(), req.apiPath(), req.parentId(), req.sortOrder(), req.enabled());
        auditService.createAudit(AuditDomainType.MENU, AuditActionType.UPDATE, menu.getId(), "MENU 수정");
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.MENU_NOT_FOUND));
        menu.delete();
        auditService.createAudit(AuditDomainType.MENU, AuditActionType.DELETE, menu.getId(), "MENU 삭제");
    }

    private void validateMenuPaths(String routePath, String apiPath, Long menuId) {
        boolean routePathExists = menuId == null
            ? menuRepository.existsByRoutePath(routePath)
            : menuRepository.existsByRoutePathAndIdNot(routePath, menuId);

        boolean apiPathExists = menuId == null
            ? menuRepository.existsByApiPath(apiPath)
            : menuRepository.existsByApiPathAndIdNot(apiPath, menuId);

        if (routePathExists || apiPathExists) {
            throw new CustomException(AuthErrorCode.MENU_ALREADY_EXISTS);
        }
    }

    private void validateParentMenu(Long parentId, Long menuId) {
        if (parentId == null) {
            return;
        }

        if (parentId.equals(menuId)) {
            throw new CustomException(AuthErrorCode.INVALID_PARENT_MENU);
        }

        Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(parentId))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_PARENT_MENU));
    }

    private List<MenuRecord.TreeResponse> buildTree(Long parentId, List<Menu> menus) {
        return menus.stream()
                    .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                    .map(menu -> menuMapper.toTreeResponse(menu, buildTree(menu.getId(), menus)))
                    .toList();
    }
}
