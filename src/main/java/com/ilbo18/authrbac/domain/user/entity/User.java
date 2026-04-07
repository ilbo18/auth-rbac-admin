package com.ilbo18.authrbac.domain.user.entity;

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
 * 사용자 정보를 관리하는 엔티티
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 ID */
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    /** 비밀번호 */
    @Column(nullable = false, length = 255)
    private String password;

    /** 사용자명 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 역할 ID */
    @Column(nullable = false)
    private Long roleId;

    /** 사용 여부 */
    @Column(nullable = false)
    private Boolean enabled;

    /** 사용자 정보 수정 */
    public void update(String loginId, String password, String name, Long roleId, Boolean enabled) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.roleId = roleId;
        this.enabled = enabled;
    }
}
