package com.soomgil.global.config;

import com.soomgil.collaboration.infrastructure.websocket.CollaborationSessionHeaderInterceptor;
import com.soomgil.collaboration.infrastructure.websocket.TripSubscriptionInterceptor;
import com.soomgil.collaboration.infrastructure.websocket.WebSocketJwtAuthenticationInterceptor;
import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 협업 broadcast 채널을 구성한다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final CorsProperties corsProperties;
	private final WebSocketJwtAuthenticationInterceptor authenticationInterceptor;
	private final TripSubscriptionInterceptor subscriptionInterceptor;
	private final CollaborationSessionHeaderInterceptor sessionHeaderInterceptor;

	public WebSocketConfig(
		CorsProperties corsProperties,
		WebSocketJwtAuthenticationInterceptor authenticationInterceptor,
		TripSubscriptionInterceptor subscriptionInterceptor,
		CollaborationSessionHeaderInterceptor sessionHeaderInterceptor
	) {
		this.corsProperties = Objects.requireNonNull(corsProperties, "corsProperties must not be null");
		this.authenticationInterceptor = Objects.requireNonNull(
			authenticationInterceptor,
			"authenticationInterceptor must not be null"
		);
		this.subscriptionInterceptor = Objects.requireNonNull(
			subscriptionInterceptor,
			"subscriptionInterceptor must not be null"
		);
		this.sessionHeaderInterceptor = Objects.requireNonNull(
			sessionHeaderInterceptor,
			"sessionHeaderInterceptor must not be null"
		);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		var endpoint = registry.addEndpoint("/ws");
		if (corsProperties.allowedOriginPatternList().isEmpty()) {
			endpoint.setAllowedOrigins(corsProperties.allowedOriginList().toArray(String[]::new));
		} else {
			endpoint.setAllowedOriginPatterns(corsProperties.allowedOriginPatternList().toArray(String[]::new));
		}
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authenticationInterceptor, subscriptionInterceptor);
	}

	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) {
		registration.interceptors(sessionHeaderInterceptor);
	}
}
