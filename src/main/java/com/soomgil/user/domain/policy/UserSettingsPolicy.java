package com.soomgil.user.domain.policy;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.domain.model.UserException;
import java.time.ZoneId;
import java.util.Set;
import java.util.TimeZone;

/**
 * 사용자 설정 값의 도메인 검증 규칙.
 *
 * <p>API DTO의 bean validation({@code @Size}, {@code @NotBlank})은 형식만 검증한다.
 * 본 policy는 비즈니스 의미를 가진 값(유효한 timezone, 지원 언어 코드)을 추가로 검증하고,
 * 위반 시 {@link UserException}을 던져 {@code ProblemDetails}로 변환한다.
 */
public final class UserSettingsPolicy {

	/**
	 * 지원하는 표시 언어 코드 집합.
	 *
	 * <p>MVP에서는 한국어/영어만 지원한다. 코드 추가 시 프론트엔드 번역 리소스와 함께 확장한다.
	 */
	public static final Set<String> SUPPORTED_LANGUAGES = Set.of("ko", "en");

	private UserSettingsPolicy() {
	}

	/**
	 * timezone 문자열이 유효한 {@link ZoneId}인지 검증한다.
	 *
	 * @param timezone 검증할 timezone 식별자
	 * @throws UserException {@code INVALID_TIMEZONE} - 유효하지 않은 timezone인 경우
	 */
	public static void validateTimezone(String timezone) {
		try {
			TimeZone tz = TimeZone.getTimeZone(ZoneId.of(timezone));
			if (!tz.getID().equals(timezone) && !tz.getID().startsWith("GMT") && !timezone.startsWith("GMT")) {
				throw new UserException(ErrorCode.INVALID_TIMEZONE,
					"Unsupported timezone: " + timezone);
			}
		} catch (java.time.zone.ZoneRulesException ex) {
			throw new UserException(ErrorCode.INVALID_TIMEZONE, "Unsupported timezone: " + timezone);
		}
	}

	/**
	 * 표시 언어 코드가 지원 목록에 포함되는지 검증한다.
	 *
	 * @param language 검증할 언어 코드
	 * @throws UserException {@code INVALID_DISPLAY_LANGUAGE} - 지원하지 않는 언어인 경우
	 */
	public static void validateDisplayLanguage(String language) {
		if (!SUPPORTED_LANGUAGES.contains(language)) {
			throw new UserException(ErrorCode.INVALID_DISPLAY_LANGUAGE,
				"Unsupported display language: " + language);
		}
	}
}
