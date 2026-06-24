package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.global.security.JwtToCurrentUserAuthenticationConverter;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP CONNECT frame의 Bearer token을 검증해 WebSocket Principal로 설정한다.
 */
@Component
public class WebSocketJwtAuthenticationInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtDecoder jwtDecoder;
	private final JwtToCurrentUserAuthenticationConverter jwtAuthenticationConverter;

	public WebSocketJwtAuthenticationInterceptor(
		JwtDecoder jwtDecoder,
		JwtToCurrentUserAuthenticationConverter jwtAuthenticationConverter
	) {
		this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "jwtDecoder must not be null");
		this.jwtAuthenticationConverter = Objects.requireNonNull(
			jwtAuthenticationConverter,
			"jwtAuthenticationConverter must not be null"
		);
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
			return message;
		}

		String token = bearerToken(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));
		if (token == null) {
			return message;
		}

		Jwt jwt = jwtDecoder.decode(token);
		AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
		accessor.setUser(authentication);
		return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
	}

	private String bearerToken(String authorization) {
		if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		return StringUtils.hasText(token) ? token : null;
	}
}
