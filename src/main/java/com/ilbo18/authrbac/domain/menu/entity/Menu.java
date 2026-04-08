package com.ilbo18.authrbac.domain.menu.entity;

import com.ilbo18.authrbac.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 화면 이동 경로와 API 인가 기준을 함께 관리하는 메뉴 엔티티
 */
@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 메뉴명 */
    @Column(nullable = false, length = 100)
    private String name;

    /** UI 또는 메뉴 이동 경로 */
    @Column(name = "route_path", nullable = false, unique = true, length = 255)
    private String routePath;

    /** permission 인가 기준 경로 */
    @Column(name = "api_path", nullable = false, unique = true, length = 255)
    private String apiPath;

    /** 상위 메뉴 ID */
    @Column
    private Long parentId;

    /** 정렬 순서 */
    @Column(nullable = false)
    private Integer sortOrder;

    /** 사용 여부 */
    @Column(nullable = false)
    private Boolean enabled;

    public void update(String name, String routePath, String apiPath, Long parentId, Integer sortOrder, Boolean enabled) {
        this.name = name;
        this.routePath = routePath;
        this.apiPath = apiPath;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
    }
}
