package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.collaboration.infrastructure.web.HttpCollaborationSessionIdProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * STOMP 연결 완료 frame에 서버 WebSocket session ID를 노출한다.
 */
@Component
public class CollaborationSessionHeaderInterceptor implements ChannelInterceptor {

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (!StompCommand.CONNECTED.equals(accessor.getCommand()) || accessor.getSessionId() == null) {
			return message;
		}
		accessor.setNativeHeader(HttpCollaborationSessionIdProvider.SESSION_HEADER, accessor.getSessionId());
		return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
	}
}
