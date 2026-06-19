package com.soomgil.user.application.handler;

import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserProfile;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.application.command.UpdateMeCommand;
import com.soomgil.user.domain.model.UserException;
import com.soomgil.user.domain.model.UserProfileRecord;
import com.soomgil.user.domain.model.UserSettingsRecord;
import com.soomgil.user.domain.policy.UserProfilePolicy;
import com.soomgil.user.infrastructure.persistence.UserMeMapper;
import com.soomgil.user.infrastructure.persistence.UserMeSettingsMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 로그인 사용자의 프로필을 부분 update한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>현재 profile row 조회. 없으면 {@code PROFILE_NOT_FOUND}.</li>
 *   <li>command의 {@code null}이 아닌 필드를 merge. {@code displayName}/{@code bio} 정책 검증.</li>
 *   <li>profile row update.</li>
 *   <li>account 정보({@code auth.users}) + primary email({@code auth.user_email_addresses}) +
 *       settings({@code auth.user_settings})를 함께 읽어 {@link User} 응답 조립.</li>
 * </ol>
 *
 * <p>account와 email은 {@code auth} 모듈의 mapper를 직접 사용한다. user 도메인의 view를
 * 조립하기 위해 auth persistence를 읽는 것은 {@code auth.GetCurrentUserQueryHandler}와
 * 동일한 패턴이다.
 */
@Component
@Transactional
public class UpdateMeCommandHandler implements CommandHandler<UpdateMeCommand, User> {

	private final UserMeMapper userMeMapper;
	private final UserMeSettingsMapper userSettingsMapper;
	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;

	public UpdateMeCommandHandler(
		UserMeMapper userMeMapper,
		UserMeSettingsMapper userSettingsMapper,
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper
	) {
		this.userMeMapper = userMeMapper;
		this.userSettingsMapper = userSettingsMapper;
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
	}

	@Override
	public User handle(UpdateMeCommand command) {
		UserProfileRecord current = userMeMapper.findFull(command.userId())
			.orElseThrow(() -> new UserException(ErrorCode.PROFILE_NOT_FOUND,
				"User profile row not found for user: " + command.userId()));

		String displayName = command.displayName() != null
			? command.displayName() : current.displayName();
		String bio = command.bio() != null ? command.bio() : current.bio();
		UUID profileMediaFileId = command.profileMediaFileId() != null
			? command.profileMediaFileId() : current.profileMediaFileId();
		UserProfileVisibility visibility = command.profileVisibility() != null
			? command.profileVisibility() : current.profileVisibility();

		UserProfilePolicy.validateDisplayName(displayName);
		UserProfilePolicy.validateBio(bio);

		UserProfileRecord merged = new UserProfileRecord(
			command.userId(),
			displayName,
			current.profileImageUrl(),
			profileMediaFileId,
			bio,
			visibility
		);

		int updated = userMeMapper.updateRecord(merged);
		if (updated == 0) {
			throw new UserException(ErrorCode.PROFILE_NOT_FOUND,
				"User profile row not found for user: " + command.userId());
		}

		return buildUserResponse(command.userId(), merged);
	}

	private User buildUserResponse(UUID userId, UserProfileRecord profileRecord) {
		AuthUser user = userMapper.findById(userId)
			.orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

		EmailAddress email = emailAddressMapper.findPrimaryByUserId(userId).orElse(null);
		String primaryEmail = email != null ? email.email() : null;
		OffsetDateTime emailVerifiedAt = email != null && email.verifiedAt() != null
			? OffsetDateTime.ofInstant(email.verifiedAt(), ZoneOffset.UTC)
			: null;

		UserProfile profile = new UserProfile(
			profileRecord.displayName(),
			profileRecord.profileImageUrl(),
			profileRecord.profileMediaFileId(),
			profileRecord.bio(),
			profileRecord.profileVisibility()
		);

		UserSettingsRecord settingsRecord = userSettingsMapper.findByUserId(userId)
			.orElseGet(() -> new UserSettingsRecord(userId, "ko", "Asia/Seoul", false, null, null, true));
		UserSettings settings = new UserSettings(
			settingsRecord.displayLanguage(),
			settingsRecord.timezone(),
			settingsRecord.marketingEmailOptIn(),
			settingsRecord.marketingEmailOptedInAt(),
			settingsRecord.marketingEmailOptedOutAt(),
			settingsRecord.tripInviteEmailOptIn()
		);

		OffsetDateTime createdAt = user.createdAt() != null
			? OffsetDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC)
			: OffsetDateTime.now(ZoneOffset.UTC);

		return new User(
			user.id(),
			primaryEmail,
			emailVerifiedAt,
			com.soomgil.user.api.dto.UserStatus.valueOf(user.status().name()),
			null,
			null,
			null,
			profile,
			settings,
			createdAt
		);
	}
}
