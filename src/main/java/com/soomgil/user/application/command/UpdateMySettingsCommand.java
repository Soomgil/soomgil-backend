package com.soomgil.user.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.user.api.dto.UserSettings;
import java.util.UUID;

/**
 * 현재 로그인 사용자의 설정을 부분 update하는 명령.
 *
 * <p>각 필드는 {@code null}이면 "변경 없음"을 의미한다. {@code marketingEmailOptIn}이
 * {@code false}에서 {@code true}로 전환되면 {@code marketingEmailOptedInAt}를,
 * 반대 방향이면 {@code marketingEmailOptedOutAt}를 handler가 자동으로 기록한다.
 *
 * <p>{@code timezone}은 유효한 {@link java.time.ZoneId}, {@code displayLanguage}는
 * 지원 언어 코드({@code ko}, {@code en})만 허용한다.
 *
 * @param userId 현재 로그인 사용자 식별자
 * @param displayLanguage 표시 언어 코드. {@code null}이면 변경 없음
 * @param timezone timezone 식별자. {@code null}이면 변경 없음
 * @param marketingEmailOptIn 마케팅 이메일 수신 여부. {@code null}이면 변경 없음
 * @param tripInviteEmailOptIn 여행 초대 이메일 수신 여부. {@code null}이면 변경 없음
 */
public record UpdateMySettingsCommand(
	UUID userId,
	String displayLanguage,
	String timezone,
	Boolean marketingEmailOptIn,
	Boolean tripInviteEmailOptIn
) implements Command<UserSettings> {
}
