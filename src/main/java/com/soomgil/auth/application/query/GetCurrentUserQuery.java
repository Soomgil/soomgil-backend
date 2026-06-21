package com.soomgil.auth.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.user.api.dto.User;
import java.util.UUID;

/**
 * 현재 로그인 사용자 프로필 조회 요청.
 *
 * @param userId 현재 사용자 식별자
 */
public record GetCurrentUserQuery(UUID userId) implements Query<User> {
}
