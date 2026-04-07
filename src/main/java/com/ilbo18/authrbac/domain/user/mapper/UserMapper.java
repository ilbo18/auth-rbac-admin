package com.ilbo18.authrbac.domain.user.mapper;

import com.ilbo18.authrbac.domain.user.entity.User;
import com.ilbo18.authrbac.domain.user.record.UserRecord;
import org.springframework.stereotype.Component;

/**
 * User 엔티티와 DTO 간 변환을 담당하는 매퍼
 */
@Component
public class UserMapper {

    /** 사용자 생성 요청 DTO를 User Entity로 변환 */
    public User toEntity(UserRecord.Create req, String loginId, String encodedPassword) {
        Boolean enabled = (req.enabled() != null) ? req.enabled() : Boolean.TRUE;

        return User.builder()
                   .loginId(loginId)
                   .password(encodedPassword)
                   .name(req.name())
                   .roleId(req.roleId())
                   .enabled(enabled)
                   .build();
    }

    /** User Entity를 사용자 응답 DTO로 변환 */
    public UserRecord.Response toResponse(User user) {
        return new UserRecord.Response(
            user.getId(),
            user.getLoginId(),
            user.getName(),
            user.getRoleId(),
            user.getEnabled(),
            user.getDeleted(),
            user.getCreatedBy(),
            user.getUpdatedBy(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
