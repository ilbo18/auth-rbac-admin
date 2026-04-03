package com.ilbo18.authrbac.domain.menu.mapper;

import com.ilbo18.authrbac.domain.menu.entity.Menu;
import com.ilbo18.authrbac.domain.menu.record.MenuRecord;
import org.springframework.stereotype.Component;

/**
 * Menu 엔티티와 DTO 간 변환을 담당하는 매퍼
 */
@Component
public class MenuMapper {

    /** 메뉴 생성 요청 DTO를 Menu Entity로 변환 */
    public Menu toEntity(MenuRecord.Create req) {
        Boolean enabled = (req.enabled() != null) ? req.enabled() : Boolean.TRUE;

        return Menu.builder()
                   .name(req.name())
                   .path(req.path())
                   .parentId(req.parentId())
                   .sortOrder(req.sortOrder())
                   .enabled(enabled)
                   .build();
    }

    /** Menu Entity를 메뉴 응답 DTO로 변환 */
    public MenuRecord.Response toResponse(Menu menu) {
        return new MenuRecord.Response(
            menu.getId(),
            menu.getName(),
            menu.getPath(),
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
}
