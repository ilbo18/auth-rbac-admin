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
 * 메뉴 정보를 관리하는 엔티티
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

    /** 메뉴 경로 */
    @Column(nullable = false, unique = true, length = 255)
    private String path;

    /** 상위 메뉴 ID */
    @Column
    private Long parentId;

    /** 정렬 순서 */
    @Column(nullable = false)
    private Integer sortOrder;

    /** 사용 여부 */
    @Column(nullable = false)
    private Boolean enabled;

    /** 메뉴 정보 수정 */
    public void update(String name, String path, Long parentId, Integer sortOrder, Boolean enabled) {
        this.name = name;
        this.path = path;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
    }
}
