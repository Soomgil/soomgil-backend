package com.soomgil.user.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.application.command.UpdateMySettingsCommand;
import com.soomgil.user.domain.model.UserException;
import com.soomgil.user.domain.model.UserSettingsRecord;
import com.soomgil.user.domain.policy.UserSettingsPolicy;
import com.soomgil.user.infrastructure.persistence.UserMeSettingsMapper;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 로그인 사용자의 설정을 부분 update한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>현재 설정 row 조회. 없으면 {@code PROFILE_NOT_FOUND}.</li>
 *   <li>command의 {@code null}이 아닌 필드를 row에 merge.</li>
 *   <li>{@code timezone}/{@code displayLanguage} 변경 시 policy로 유효성 검증.</li>
 *   <li>{@code marketingEmailOptIn} 전환 방향에 따라 {@code opted_in_at}/{@code opted_out_at} 갱신.</li>
 *   <li>전체 row를 다시 쓰고 갱신된 {@link UserSettings} 반환.</li>
 * </ol>
 */
@Component
@Transactional
public class UpdateMySettingsCommandHandler implements CommandHandler<UpdateMySettingsCommand, UserSettings> {

	private final UserMeSettingsMapper settingsMapper;

	public UpdateMySettingsCommandHandler(UserMeSettingsMapper settingsMapper) {
		this.settingsMapper = settingsMapper;
	}

	@Override
	public UserSettings handle(UpdateMySettingsCommand command) {
		UserSettingsRecord current = settingsMapper.findByUserId(command.userId())
			.orElseThrow(() -> new UserException(ErrorCode.PROFILE_NOT_FOUND,
				"User settings row not found for user: " + command.userId()));

		String displayLanguage = command.displayLanguage() != null
			? command.displayLanguage() : current.displayLanguage();
		String timezone = command.timezone() != null
			? command.timezone() : current.timezone();

		UserSettingsPolicy.validateDisplayLanguage(displayLanguage);
		UserSettingsPolicy.validateTimezone(timezone);

		boolean marketingOptIn = command.marketingEmailOptIn() != null
			? command.marketingEmailOptIn() : current.marketingEmailOptIn();
		boolean tripInviteOptIn = command.tripInviteEmailOptIn() != null
			? command.tripInviteEmailOptIn() : current.tripInviteEmailOptIn();

		OffsetDateTime marketingOptedInAt = current.marketingEmailOptedInAt();
		OffsetDateTime marketingOptedOutAt = current.marketingEmailOptedOutAt();
		OffsetDateTime now = OffsetDateTime.now();

		boolean marketingFlipped = (command.marketingEmailOptIn() != null)
			&& (command.marketingEmailOptIn() != current.marketingEmailOptIn());
		if (marketingFlipped) {
			if (marketingOptIn) {
				marketingOptedInAt = now;
			} else {
				marketingOptedOutAt = now;
			}
		}

		UserSettingsRecord merged = new UserSettingsRecord(
			command.userId(),
			displayLanguage,
			timezone,
			marketingOptIn,
			marketingOptedInAt,
			marketingOptedOutAt,
			tripInviteOptIn
		);

		int updated = settingsMapper.updateRecord(command.userId(), merged);
		if (updated == 0) {
			throw new UserException(ErrorCode.PROFILE_NOT_FOUND,
				"User settings row not found for user: " + command.userId());
		}

		return new UserSettings(
			merged.displayLanguage(),
			merged.timezone(),
			merged.marketingEmailOptIn(),
			merged.marketingEmailOptedInAt(),
			merged.marketingEmailOptedOutAt(),
			merged.tripInviteEmailOptIn()
		);
	}
}
