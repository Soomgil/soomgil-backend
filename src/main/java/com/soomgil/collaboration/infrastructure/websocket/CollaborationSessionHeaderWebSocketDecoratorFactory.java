package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.collaboration.infrastructure.web.HttpCollaborationSessionIdProvider;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

/**
 * STOMP {@code CONNECTED} frame에 서버 WebSocket session ID를 추가한다.
 *
 * <p>Spring은 client outbound channel 이후에 내부 {@code CONNECT_ACK} 메시지를 실제
 * {@code CONNECTED} frame으로 변환하므로 channel interceptor에서는 native header가 유실된다.
 * 최종 WebSocket text frame을 전송하는 시점에 session ID를 넣어 브라우저와 HTTP mutation이
 * 동일한 협업 session을 사용하도록 한다.
 */
@Component
public class CollaborationSessionHeaderWebSocketDecoratorFactory implements WebSocketHandlerDecoratorFactory {

	@Override
	public WebSocketHandler decorate(WebSocketHandler handler) {
		return new SessionHeaderHandlerDecorator(handler);
	}

	private static final class SessionHeaderHandlerDecorator extends WebSocketHandlerDecorator {

		private final ConcurrentHashMap<String, WebSocketSession> decoratedSessions = new ConcurrentHashMap<>();

		private SessionHeaderHandlerDecorator(WebSocketHandler delegate) {
			super(delegate);
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			super.afterConnectionEstablished(decorated(session));
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
			super.handleMessage(decorated(session), message);
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
			super.handleTransportError(decorated(session), exception);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			WebSocketSession decorated = decorated(session);
			try {
				super.afterConnectionClosed(decorated, closeStatus);
			}
			finally {
				decoratedSessions.remove(session.getId());
			}
		}

		private WebSocketSession decorated(WebSocketSession session) {
			return decoratedSessions.computeIfAbsent(session.getId(), ignored -> new SessionHeaderSessionDecorator(session));
		}
	}

	private static final class SessionHeaderSessionDecorator extends WebSocketSessionDecorator {

		private static final String CONNECTED_PREFIX = "CONNECTED\n";

		private SessionHeaderSessionDecorator(WebSocketSession delegate) {
			super(delegate);
		}

		@Override
		public void sendMessage(WebSocketMessage<?> message) throws IOException {
			if (message instanceof TextMessage textMessage) {
				super.sendMessage(withSessionHeader(textMessage));
				return;
			}
			super.sendMessage(message);
		}

		private TextMessage withSessionHeader(TextMessage message) {
			String payload = message.getPayload();
			String headerName = HttpCollaborationSessionIdProvider.SESSION_HEADER;
			if (!payload.startsWith(CONNECTED_PREFIX) || payload.contains("\n" + headerName + ":")) {
				return message;
			}
			String decoratedPayload = CONNECTED_PREFIX
				+ headerName + ":" + getId() + "\n"
				+ payload.substring(CONNECTED_PREFIX.length());
			return new TextMessage(decoratedPayload);
		}
	}
}
