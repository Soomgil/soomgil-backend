package com.soomgil.collaboration.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HttpCollaborationSessionIdProviderTest {

	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private final CollaborationWebSocketSessionRegistry registry = new CollaborationWebSocketSessionRegistry();
	private final HttpCollaborationSessionIdProvider provider = new HttpCollaborationSessionIdProvider(registry);
	private final Principal principal = () -> USER_ID.toString();

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
}
