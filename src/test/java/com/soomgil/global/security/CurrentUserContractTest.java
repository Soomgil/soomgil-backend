package com.soomgil.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurrentUserContractTest {

	@Test
	void currentUserRequiresUserId() {
		assertThatNullPointerException()
			.isThrownBy(() -> new CurrentUser(null, "user@example.com"));
	}

	@Test
	void currentUserProviderReturnsAuthenticatedUser() {
		UUID userId = UUID.randomUUID();
		CurrentUserProvider provider = () -> new CurrentUser(userId, "user@example.com");

		CurrentUser currentUser = provider.currentUser();

		assertThat(currentUser.userId()).isEqualTo(userId);
		assertThat(currentUser.email()).isEqualTo("user@example.com");
		assertThat(provider.currentUserId()).isEqualTo(userId);
	}
}
