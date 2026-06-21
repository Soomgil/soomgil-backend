package com.soomgil.user.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link UserSettingsPolicy} 단위 테스트.
 *
 * <p>timezone과 표시 언어 코드의 유효성 검증을 검증한다.
 */
class UserSettingsPolicyTest {

	@Test
	@DisplayName("유효한 IANA timezone은 통과한다")
	void acceptsValidTimezone() {
		assertThatCode(() -> UserSettingsPolicy.validateTimezone("Asia/Seoul"))
			.doesNotThrowAnyException();
		assertThatCode(() -> UserSettingsPolicy.validateTimezone("Europe/London"))
			.doesNotThrowAnyException();
		assertThatCode(() -> UserSettingsPolicy.validateTimezone("America/New_York"))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("유효하지 않은 timezone은 INVALID_TIMEZONE 예외를 던진다")
	void rejectsInvalidTimezone() {
		assertThatThrownBy(() -> UserSettingsPolicy.validateTimezone("Foo/Bar"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_TIMEZONE);
	}

	@Test
	@DisplayName("ko, en은 지원 언어로 통과한다")
	void acceptsSupportedLanguage() {
		assertThatCode(() -> UserSettingsPolicy.validateDisplayLanguage("ko"))
			.doesNotThrowAnyException();
		assertThatCode(() -> UserSettingsPolicy.validateDisplayLanguage("en"))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("지원하지 않는 언어 코드는 INVALID_DISPLAY_LANGUAGE 예외를 던진다")
	void rejectsUnsupportedLanguage() {
		assertThatThrownBy(() -> UserSettingsPolicy.validateDisplayLanguage("ja"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_DISPLAY_LANGUAGE);
	}
}
