package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.global.error.BusinessException;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 인증 사용자와 여행방 멤버십을 기준으로 STOMP 연결과 구독을 제한한다.
 */
@Component
public class TripSubscriptionInterceptor implements ChannelInterceptor {

	private static final Pattern TRIP_TOPIC = Pattern.compile(
		"^/topic/trips/([0-9a-fA-F-]{36})/(collaboration|itinerary|map-drawings|route-matching)$"
	);

	private final TripAccessGuard tripAccessGuard;
	private final CollaborationWebSocketSessionRegistry sessionRegistry;

	public TripSubscriptionInterceptor(
		TripAccessGuard tripAccessGuard,
		CollaborationWebSocketSessionRegistry sessionRegistry
	) {
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			sessionRegistry.register(accessor.getSessionId(), requireUser(accessor.getUser()));
		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			authorizeSubscription(accessor);
		}
		if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
			sessionRegistry.unregister(accessor.getSessionId());
		}
		return message;
	}

	private void authorizeSubscription(StompHeaderAccessor accessor) {
		UUID userId = requireUser(accessor.getUser());
		String destination = accessor.getDestination();
		Matcher matcher = destination == null ? null : TRIP_TOPIC.matcher(destination);
		if (matcher == null || !matcher.matches()) {
			throw new AccessDeniedException("Subscription destination is not allowed.");
		}
		UUID tripId;
		try {
			tripId = UUID.fromString(matcher.group(1));
		}
		catch (IllegalArgumentException exception) {
			throw new AccessDeniedException("Trip subscription ID must be a UUID.", exception);
		}
		try {
			tripAccessGuard.requireActiveMember(tripId, userId);
		}
		catch (BusinessException exception) {
			throw new AccessDeniedException("Trip member subscription is required.", exception);
		}
	}

	private UUID requireUser(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new AccessDeniedException("Authenticated WebSocket connection is required.");
		}
		try {
			return UUID.fromString(principal.getName());
		}
		catch (IllegalArgumentException exception) {
			throw new AccessDeniedException("Authenticated user ID must be a UUID.", exception);
		}
	}
}
