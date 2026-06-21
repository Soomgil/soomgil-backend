package com.soomgil.community.application.service;

import com.soomgil.auth.application.handler.FindUserRolesQueryHandler;
import com.soomgil.auth.application.query.FindUserRolesQuery;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.global.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 모더레이션 권한을 검증한다.
 *
 * <p>auth 모듈의 query interface를 통해 {@code MODERATOR} 역할이 있는지 확인한다.
 * 권한이 없으면 {@link ErrorCode#MODERATION_ACCESS_DENIED}를 던진다.
 */
@Component
public class ModerationAccessGuard {

	private final FindUserRolesQueryHandler userRolesQueryHandler;

	public ModerationAccessGuard(FindUserRolesQueryHandler userRolesQueryHandler) {
		this.userRolesQueryHandler = userRolesQueryHandler;
	}

	/**
	 * 사용자가 MODERATOR 역할을 가졌는지 검증한다.
	 *
	 * @param userId 검증할 사용자
	 * @throws CommunityException MODERATOR 역할이 없는 경우
	 */
	public void requireModerator(UUID userId) {
		var roles = userRolesQueryHandler.handle(new FindUserRolesQuery(userId));
		if (!roles.contains("MODERATOR")) {
			throw new CommunityException(ErrorCode.MODERATION_ACCESS_DENIED);
		}
	}
}
