package com.soomgil.user.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link UserProfilePolicy} 단위 테스트.
 */
class UserProfilePolicyTest {

	@Test
	@DisplayName("1~80자 사이의 표시 이름은 통과한다")
	void acceptsValidDisplayName() {
		assertThatCode(() -> UserProfilePolicy.validateDisplayName("민지"))
			.doesNotThrowAnyException();
		assertThatCode(() -> UserProfilePolicy.validateDisplayName("a".repeat(80)))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("빈 표시 이름은 거절한다")
	void rejectsEmptyDisplayName() {
		assertThatThrownBy(() -> UserProfilePolicy.validateDisplayName(""))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	@DisplayName("81자 이상 표시 이름은 거절한다")
	void rejectsTooLongDisplayName() {
		assertThatThrownBy(() -> UserProfilePolicy.validateDisplayName("a".repeat(81)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	@DisplayName("null bio는 통과한다")
	void acceptsNullBio() {
		assertThatCode(() -> UserProfilePolicy.validateBio(null))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("500자 이하 bio는 통과한다")
	void acceptsBioUpTo500() {
		assertThatCode(() -> UserProfilePolicy.validateBio("a".repeat(500)))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("501자 이상 bio는 거절한다")
	void rejectsBioOver500() {
		assertThatThrownBy(() -> UserProfilePolicy.validateBio("a".repeat(501)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}
}
