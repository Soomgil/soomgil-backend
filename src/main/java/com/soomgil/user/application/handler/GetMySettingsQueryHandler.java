package com.soomgil.user.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.application.query.GetMySettingsQuery;
import com.soomgil.user.domain.model.UserSettingsRecord;
import com.soomgil.user.infrastructure.persistence.UserMeSettingsMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 로그인 사용자의 설정을 조회한다.
 *
 * <p>{@link UserMeSettingsMapper}로 {@code auth.user_settings} row를 읽어
 * {@link UserSettings} 응답으로 변환한다. row가 없으면 기본값({@code ko},
 * {@code Asia/Seoul}, 마케팅 수신 false, 여행 초대 수신 true)으로 대체한다.
 * 회원가입 흐름에서 설정 row를 항상 생성하므로 기본값 분기는 예상치 못한 결손에 대한 안전망이다.
 */
@Component
@Transactional(readOnly = true)
public class GetMySettingsQueryHandler implements QueryHandler<GetMySettingsQuery, UserSettings> {

	private static final String DEFAULT_LANGUAGE = "ko";
	private static final String DEFAULT_TIMEZONE = "Asia/Seoul";

	private final UserMeSettingsMapper userSettingsMapper;

	public GetMySettingsQueryHandler(UserMeSettingsMapper userSettingsMapper) {
		this.userSettingsMapper = userSettingsMapper;
	}

	@Override
	public UserSettings handle(GetMySettingsQuery query) {
		UserSettingsRecord record = userSettingsMapper.findByUserId(query.userId())
			.orElseGet(() -> defaultSettings(query.userId()));

		return new UserSettings(
			record.displayLanguage(),
			record.timezone(),
			record.marketingEmailOptIn(),
			record.marketingEmailOptedInAt(),
			record.marketingEmailOptedOutAt(),
			record.tripInviteEmailOptIn()
		);
	}

	private UserSettingsRecord defaultSettings(UUID userId) {
		return new UserSettingsRecord(
			userId,
			DEFAULT_LANGUAGE,
			DEFAULT_TIMEZONE,
			false,
			null,
			null,
			true
		);
	}
}
