package com.soomgil.collaboration.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soomgil.collaboration.infrastructure.web.HttpCollaborationSessionIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class CollaborationSessionHeaderInterceptorTest {

	private final CollaborationSessionHeaderInterceptor interceptor = new CollaborationSessionHeaderInterceptor();
	private final MessageChannel channel = mock(MessageChannel.class);

	@Test
	void addsServerSessionIdToConnectedFrame() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setSessionId("session-1");
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, channel);

		StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
		assertThat(resultAccessor.getFirstNativeHeader(HttpCollaborationSessionIdProvider.SESSION_HEADER))
			.isEqualTo("session-1");
	}
}
