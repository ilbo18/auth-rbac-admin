package com.ilbo18.authrbac.domain.permission.entity;

import com.ilbo18.authrbac.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권한 정보를 관리하는 엔티티
 */
@Getter
@Entity
@Table(name = "permissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 역할 ID */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** 메뉴 ID */
    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    /** 조회 권한 */
    @Column(nullable = false)
    private Boolean canRead;

    /** 생성 권한 */
    @Column(nullable = false)
    private Boolean canCreate;

    /** 수정 권한 */
    @Column(nullable = false)
    private Boolean canUpdate;

    /** 삭제 권한 */
    @Column(nullable = false)
    private Boolean canDelete;

    /** 사용 여부 */
    @Column(nullable = false)
    private Boolean enabled;

    /** 권한 정보 수정 */
    public void update(
        Long roleId,
        Long menuId,
        Boolean canRead,
        Boolean canCreate,
        Boolean canUpdate,
        Boolean canDelete,
        Boolean enabled
    ) {
        this.roleId = roleId;
        this.menuId = menuId;
        this.canRead = canRead;
        this.canCreate = canCreate;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
        this.enabled = enabled;
    }
}
