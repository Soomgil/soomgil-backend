package com.soomgil.collaboration.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.infrastructure.web.HttpCollaborationSessionIdProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

class CollaborationSessionHeaderWebSocketDecoratorFactoryTest {

	@Test
	void addsServerSessionIdToFinalConnectedFrame() throws Exception {
		WebSocketHandler delegateHandler = mock(WebSocketHandler.class);
		WebSocketSession delegateSession = mock(WebSocketSession.class);
		when(delegateSession.getId()).thenReturn("session-1");
		WebSocketHandler decoratedHandler = new CollaborationSessionHeaderWebSocketDecoratorFactory()
			.decorate(delegateHandler);
		decoratedHandler.afterConnectionEstablished(delegateSession);

		ArgumentCaptor<WebSocketSession> sessionCaptor = ArgumentCaptor.forClass(WebSocketSession.class);
		verify(delegateHandler).afterConnectionEstablished(sessionCaptor.capture());
		WebSocketSession decoratedSession = sessionCaptor.getValue();
		decoratedSession.sendMessage(new TextMessage("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0"));

		ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
		verify(delegateSession).sendMessage(messageCaptor.capture());
		assertThat(messageCaptor.getValue().getPayload().toString()).contains(
			"\n" + HttpCollaborationSessionIdProvider.SESSION_HEADER + ":session-1\n"
		);
	}

	@Test
	void leavesNonConnectedFramesUnchanged() throws Exception {
		WebSocketHandler delegateHandler = mock(WebSocketHandler.class);
		WebSocketSession delegateSession = mock(WebSocketSession.class);
		when(delegateSession.getId()).thenReturn("session-1");
		WebSocketHandler decoratedHandler = new CollaborationSessionHeaderWebSocketDecoratorFactory()
			.decorate(delegateHandler);
		decoratedHandler.afterConnectionEstablished(delegateSession);

		ArgumentCaptor<WebSocketSession> sessionCaptor = ArgumentCaptor.forClass(WebSocketSession.class);
		verify(delegateHandler).afterConnectionEstablished(sessionCaptor.capture());
		TextMessage original = new TextMessage("MESSAGE\ndestination:/topic/test\n\n{}\0");
		sessionCaptor.getValue().sendMessage(original);

		verify(delegateSession).sendMessage(original);
	}
}
