package com.ilbo18.authrbac.domain.auth.service;

import com.ilbo18.authrbac.global.security.AuthenticatedUser;
import com.ilbo18.authrbac.domain.auth.record.AuthRecord;

public interface AuthService {

    /** 로그인 후 access token을 발급한다. */
    AuthRecord.Token login(AuthRecord.Login req);

    /** 인증 사용자 정보를 반환한다. */
    AuthRecord.Me getMe(AuthenticatedUser authenticatedUser);
}
