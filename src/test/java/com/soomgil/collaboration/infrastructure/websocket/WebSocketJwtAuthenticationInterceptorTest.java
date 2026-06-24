package com.soomgil.collaboration.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.security.JwtToCurrentUserAuthenticationConverter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class WebSocketJwtAuthenticationInterceptorTest {

	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
	private final WebSocketJwtAuthenticationInterceptor interceptor = new WebSocketJwtAuthenticationInterceptor(
		jwtDecoder,
		new JwtToCurrentUserAuthenticationConverter()
	);
	private final MessageChannel channel = mock(MessageChannel.class);

	@Test
	void setsPrincipalFromConnectBearerToken() {
		when(jwtDecoder.decode("access-token")).thenReturn(jwt());
		Message<?> message = connectMessage("Bearer access-token");

		Message<?> authenticated = interceptor.preSend(message, channel);

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(authenticated);
		assertThat(accessor.getUser()).isNotNull();
		assertThat(accessor.getUser().getName()).isEqualTo(USER_ID.toString());
		verify(jwtDecoder).decode("access-token");
	}

	@Test
	void leavesConnectWithoutBearerTokenUnauthenticated() {
		Message<?> message = connectMessage(null);

		Message<?> unauthenticated = interceptor.preSend(message, channel);

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(unauthenticated);
		assertThat(accessor.getUser()).isNull();
	}

	private Message<?> connectMessage(String authorization) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorization != null) {
			accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private Jwt jwt() {
		Instant now = Instant.now();
		return Jwt.withTokenValue("access-token")
			.header("alg", "HS256")
			.subject(USER_ID.toString())
			.issuedAt(now)
			.expiresAt(now.plusSeconds(900))
			.issuer("soomgil")
			.build();
	}
}
