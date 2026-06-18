package com.soomgil.global.config;

import com.soomgil.collaboration.infrastructure.websocket.TripSubscriptionInterceptor;
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
	private final TripSubscriptionInterceptor subscriptionInterceptor;

	public WebSocketConfig(CorsProperties corsProperties, TripSubscriptionInterceptor subscriptionInterceptor) {
		this.corsProperties = Objects.requireNonNull(corsProperties, "corsProperties must not be null");
		this.subscriptionInterceptor = Objects.requireNonNull(
			subscriptionInterceptor,
			"subscriptionInterceptor must not be null"
		);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(corsProperties.allowedOriginList().toArray(String[]::new));
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(subscriptionInterceptor);
	}
}
