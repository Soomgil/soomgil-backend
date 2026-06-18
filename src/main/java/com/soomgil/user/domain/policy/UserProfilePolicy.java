package com.soomgil.user.domain.policy;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.domain.model.UserException;

/**
 * 사용자 프로필 값의 도메인 검증 규칙.
 *
 * <p>API DTO의 bean validation({@code @Size})과 중복되지만, 정책을 객체로 두어
 * 단위 테스트가 쉽고 비즈니스 규칙 변경 지점을 명확히 한다.
 */
public final class UserProfilePolicy {

	/** 표시 이름 최소 길이. */
	public static final int DISPLAY_NAME_MIN = 1;
	/** 표시 이름 최대 길이. {@code auth.user_profiles.display_name} 컬럼 길이와 일치. */
	public static final int DISPLAY_NAME_MAX = 80;
	/** 자기소개 최대 길이. {@code auth.user_profiles.bio} 컬럼 길이와 일치. */
	public static final int BIO_MAX = 500;

	private UserProfilePolicy() {
	}

	/**
	 * 표시 이름의 길이를 검증한다.
	 *
	 * @param displayName 검증할 표시 이름
	 * @throws UserException {@code BUSINESS_RULE_VIOLATION} - 길이가 1~80 범위를 벗어난 경우
	 */
	public static void validateDisplayName(String displayName) {
		int len = displayName == null ? 0 : displayName.length();
		if (len < DISPLAY_NAME_MIN || len > DISPLAY_NAME_MAX) {
			throw new UserException(ErrorCode.BUSINESS_RULE_VIOLATION,
				"Display name length must be between " + DISPLAY_NAME_MIN + " and " + DISPLAY_NAME_MAX);
		}
	}

	/**
	 * 자기소개의 길이를 검증한다.
	 *
	 * @param bio 검증할 자기소개. {@code null}이면 미검증
	 * @throws UserException {@code BUSINESS_RULE_VIOLATION} - 길이가 500자 초과인 경우
	 */
	public static void validateBio(String bio) {
		if (bio == null) {
			return;
		}
		if (bio.length() > BIO_MAX) {
			throw new UserException(ErrorCode.BUSINESS_RULE_VIOLATION,
				"Bio length must be at most " + BIO_MAX);
		}
	}
}
