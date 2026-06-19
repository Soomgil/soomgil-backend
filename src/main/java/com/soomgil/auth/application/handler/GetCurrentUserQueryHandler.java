package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.query.GetCurrentUserQuery;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserProfile;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.api.dto.UserStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 로그인 사용자의 프로필을 조회한다.
 *
 * <p>users, user_email_addresses, user_profiles를 조합하여 {@link User} 응답을 구성한다.
 */
@Component
@Transactional(readOnly = true)
public class GetCurrentUserQueryHandler implements QueryHandler<GetCurrentUserQuery, User> {

	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final UserProfileMapper userProfileMapper;

	public GetCurrentUserQueryHandler(
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper,
		UserProfileMapper userProfileMapper
	) {
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.userProfileMapper = userProfileMapper;
	}

	@Override
	public User handle(GetCurrentUserQuery query) {
		AuthUser user = userMapper.findById(query.userId())
			.orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

		EmailAddress emailAddress = emailAddressMapper.findPrimaryByUserId(user.id()).orElse(null);
		String email = emailAddress != null ? emailAddress.email() : null;
		OffsetDateTime emailVerifiedAt = emailAddress != null && emailAddress.verifiedAt() != null
			? OffsetDateTime.ofInstant(emailAddress.verifiedAt(), ZoneOffset.UTC)
			: null;

		String displayName = userProfileMapper.findDisplayName(user.id()).orElse(null);

		UserProfile profile = new UserProfile(
			displayName != null ? displayName : "",
			null, null, null,
			UserProfileVisibility.PUBLIC
		);
		UserSettings settings = new UserSettings("ko", "Asia/Seoul", false, null, null, true);
		OffsetDateTime createdAt = user.createdAt() != null
			? OffsetDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC)
			: OffsetDateTime.now(ZoneOffset.UTC);

		return new User(
			user.id(),
			email,
			emailVerifiedAt,
			UserStatus.valueOf(user.status().name()),
			null,
			null,
			null,
			profile,
			settings,
			createdAt
		);
	}
}
