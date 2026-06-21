package com.soomgil.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentUserProviderTest {

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void returnsTheCurrentUserPrincipalFromSpringSecurity() {
		CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "route@example.com");
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(currentUser, null, java.util.List.of())
		);

		assertThat(new SecurityContextCurrentUserProvider().currentUser()).isEqualTo(currentUser);
	}

	@Test
	void rejectsRequestsWithoutACurrentUserPrincipal() {
		assertThatThrownBy(() -> new SecurityContextCurrentUserProvider().currentUser())
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
			);
	}
}
