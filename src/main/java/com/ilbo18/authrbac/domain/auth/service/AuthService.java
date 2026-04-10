package com.ilbo18.authrbac.domain.auth.service;

import com.ilbo18.authrbac.domain.auth.record.AuthRecord;
import com.ilbo18.authrbac.global.security.AuthenticatedUser;

public interface AuthService {

    AuthRecord.Token login(AuthRecord.Login req);

    AuthRecord.Token reissue(AuthRecord.Reissue req);

    void logout(AuthenticatedUser authenticatedUser);

    AuthRecord.Me getMe(AuthenticatedUser authenticatedUser);
}
