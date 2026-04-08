package com.ilbo18.authrbac.domain.menu.mapper;

import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Menu 엔티티와 DTO 간 변환을 담당하는 매퍼
 */
@Component
public class MenuMapper {

    public Menu toEntity(MenuRecord.Create req) {
        Boolean enabled = req.enabled() != null ? req.enabled() : Boolean.TRUE;

        return Menu.builder()
                   .name(req.name())
                   .routePath(req.routePath())
                   .apiPath(req.apiPath())
                   .parentId(req.parentId())
                   .sortOrder(req.sortOrder())
                   .enabled(enabled)
                   .build();
    }

    public MenuRecord.Response toResponse(Menu menu) {
        return new MenuRecord.Response(
            menu.getId(),
            menu.getName(),
            menu.getRoutePath(),
            menu.getApiPath(),
            menu.getParentId(),
            menu.getSortOrder(),
            menu.getEnabled(),
            menu.getDeleted(),
            menu.getCreatedBy(),
            menu.getUpdatedBy(),
            menu.getCreatedAt(),
            menu.getUpdatedAt()
        );
    }

    public MenuRecord.TreeResponse toTreeResponse(Menu menu, List<MenuRecord.TreeResponse> children) {
        return new MenuRecord.TreeResponse(
            menu.getId(),
            menu.getName(),
            menu.getRoutePath(),
            menu.getApiPath(),
            menu.getParentId(),
            menu.getSortOrder(),
            menu.getEnabled(),
            children
        );
    }
}
