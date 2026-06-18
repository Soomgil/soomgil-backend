package com.soomgil.user.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * {@code auth.user_settings} row의 읽기/쓰기에 사용하는 user 도메인 persistence record.
 *
 * <p>{@link com.soomgil.user.api.dto.UserSettings} API DTO와 구분한다.
 * API DTO는 타임스탬프 일부만 노출하지만, 본 record는 persistence에서 읽어온 모든 컬럼을
 * 그대로 담아 application 계층에서 필요한 만큼 사용한다.
 *
 * @param userId 사용자 식별자
 * @param displayLanguage 표시 언어 코드. 기본 {@code ko}
 * @param timezone timezone 식별자. 기본 {@code Asia/Seoul}
 * @param marketingEmailOptIn 마케팅 이메일 수신 동의 여부
 * @param marketingEmailOptedInAt 마케팅 수신 동의 시각. 동의한 적 없으면 {@code null}
 * @param marketingEmailOptedOutAt 마케팅 수신 거부 시각. 거부한 적 없으면 {@code null}
 * @param tripInviteEmailOptIn 여행방 초대 이메일 수신 여부
 */
public record UserSettingsRecord(
	UUID userId,
	String displayLanguage,
	String timezone,
	boolean marketingEmailOptIn,
	OffsetDateTime marketingEmailOptedInAt,
	OffsetDateTime marketingEmailOptedOutAt,
	boolean tripInviteEmailOptIn
) {
}
