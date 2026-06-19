package com.soomgil.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link PasswordHasher} 단위 테스트.
 *
 * <p>BCrypt hash와 검증 로직을 Spring context 없이 검증한다.
 */
class PasswordHasherTest {

	private final PasswordHasher passwordHasher = new PasswordHasher();

	@Test
	@DisplayName("같은 비밀번호로 hash하면 매번 다른 hash가 나온다")
	void hashProducesDifferentOutputEachTime() {
		String hash1 = passwordHasher.hash("password123!");
		String hash2 = passwordHasher.hash("password123!");

		assertThat(hash1).isNotEqualTo(hash2);
	}

	@Test
	@DisplayName("raw password와 hash가 일치하면 true를 반환한다")
	void matchesReturnsTrueForCorrectPassword() {
		String hash = passwordHasher.hash("mySecretPassword");

		assertThat(passwordHasher.matches("mySecretPassword", hash)).isTrue();
	}

	@Test
	@DisplayName("잘못된 비밀번호는 false를 반환한다")
	void matchesReturnsFalseForWrongPassword() {
		String hash = passwordHasher.hash("correctPassword");

		assertThat(passwordHasher.matches("wrongPassword", hash)).isFalse();
	}
}
