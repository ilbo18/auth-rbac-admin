package com.ilbo18.authrbac.domain.user.service;

import com.ilbo18.authrbac.domain.user.record.UserRecord;

import java.util.List;

public interface UserService {

    /** 사용자 생성 */
    void createUser(UserRecord.Create req);

    /** 사용자 목록 조회 */
    List<UserRecord.Response> getUsers();

    /** 사용자 단건 조회 */
    UserRecord.Response getUser(Long id);

    /** 사용자 수정 */
    void updateUser(Long id, UserRecord.Update req);

    /** 사용자 삭제 */
    void deleteUser(Long id);
}
