package com.soomgil.collaboration.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HttpCollaborationSessionIdProviderTest {

	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private final CollaborationWebSocketSessionRegistry registry = new CollaborationWebSocketSessionRegistry();
	private final HttpCollaborationSessionIdProvider provider = new HttpCollaborationSessionIdProvider(registry);
	private final Principal principal = () -> USER_ID.toString();

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void returnsSessionOwnedByCurrentUser() {
		registry.register("session-1", USER_ID);

		assertThat(provider.requireOwnedSession(" session-1 ", principal)).isEqualTo("session-1");
	}

	@Test
	void rejectsSessionOwnedByAnotherUser() {
		registry.register("session-1", UUID.fromString("20000000-0000-0000-0000-000000000002"));

		assertThatThrownBy(() -> provider.requireOwnedSession("session-1", principal))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	@Test
	void currentSessionIdReturnsNullWhenHeaderSessionIsStale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setUserPrincipal(principal);
		request.addHeader(HttpCollaborationSessionIdProvider.SESSION_HEADER, "stale-session");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		assertThat(provider.currentSessionId()).isNull();
	}

	@Test
	void currentSessionIdReturnsOwnedSessionFromRequestHeader() {
		registry.register("session-1", USER_ID);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setUserPrincipal(principal);
		request.addHeader(HttpCollaborationSessionIdProvider.SESSION_HEADER, " session-1 ");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		assertThat(provider.currentSessionId()).isEqualTo("session-1");
	}
}
