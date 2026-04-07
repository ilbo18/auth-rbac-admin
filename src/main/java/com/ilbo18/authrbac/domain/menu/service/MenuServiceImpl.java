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
import java.util.Optional;

/**
 * 메뉴 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public void createMenu(MenuRecord.Create req) {
        if (menuRepository.existsByPath(req.path())) throw new CustomException(AuthErrorCode.MENU_ALREADY_EXISTS);

        validateParentMenu(req.parentId(), null);

        Menu menu = menuMapper.toEntity(req);

        menuRepository.save(menu);
        // 감사 로그를 저장
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

        if (menuRepository.existsByPathAndIdNot(req.path(), id)) throw new CustomException(AuthErrorCode.MENU_ALREADY_EXISTS);

        validateParentMenu(req.parentId(), id);

        menu.update(req.name(), req.path(), req.parentId(), req.sortOrder(), req.enabled());
        // 감사 로그를 저장
        auditService.createAudit(AuditDomainType.MENU, AuditActionType.UPDATE, menu.getId(), "MENU 수정");
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(id))
                            .orElseThrow(() -> new CustomException(AuthErrorCode.MENU_NOT_FOUND));
        menu.delete();
        // 감사 로그를 저장
        auditService.createAudit(AuditDomainType.MENU, AuditActionType.DELETE, menu.getId(), "MENU 삭제");
    }

    /** 상위 메뉴 유효성 검증 */
    private void validateParentMenu(Long parentId, Long menuId) {
        if (parentId == null) return;

        if (parentId.equals(menuId)) throw new CustomException(AuthErrorCode.INVALID_PARENT_MENU);

        Optional.ofNullable(menuRepository.findByIdAndDeletedFalse(parentId))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_PARENT_MENU));
    }
}
