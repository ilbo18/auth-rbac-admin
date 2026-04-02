package com.ilbo18.authrbac.domain.role.entity;

import com.ilbo18.authrbac.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 역할 정보를 관리하는 엔티티
 */
@Getter
@Entity
@Table(name = "roles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 역할 코드 */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** 역할명 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 역할 설명 */
    @Column(length = 255)
    private String description;

    /** 사용 여부 */
    @Column(nullable = false)
    private Boolean enabled;

    /** 역할 정보 수정 */
    public void update(String name, String description, Boolean enabled) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }
}
