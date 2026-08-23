package com.soomgil.collaboration.api;

import com.soomgil.collaboration.api.dto.MapCursorEvent;
import com.soomgil.collaboration.api.dto.MapCursorRequest;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** 인증 사용자 지도 커서를 최대 20Hz로 같은 여행방 presence topic에 중계한다. */
@Controller
public class MapCursorController {

	private static final long MIN_INTERVAL_NANOS = 50_000_000L;
	private final TripAccessGuard tripAccessGuard;
	private final SimpMessagingTemplate messagingTemplate;
	private final CollaborationWebSocketSessionRegistry sessionRegistry;
	private final TimeProvider timeProvider;
	private final LongSupplier nanoTime;
	private final ConcurrentHashMap<String, Long> lastSentNanos = new ConcurrentHashMap<>();

	@Autowired
	public MapCursorController(
		TripAccessGuard tripAccessGuard,
		SimpMessagingTemplate messagingTemplate,
		CollaborationWebSocketSessionRegistry sessionRegistry,
		TimeProvider timeProvider
	) {
		this(tripAccessGuard, messagingTemplate, sessionRegistry, timeProvider, System::nanoTime);
	}

	MapCursorController(
		TripAccessGuard tripAccessGuard,
		SimpMessagingTemplate messagingTemplate,
		CollaborationWebSocketSessionRegistry sessionRegistry,
		TimeProvider timeProvider,
		LongSupplier nanoTime
	) {
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
	}

	@MessageMapping("/trips/{tripId}/cursor")
	public void cursor(
		@DestinationVariable UUID tripId,
		@Payload MapCursorRequest request,
		Principal principal,
		@Header(name = SimpMessageHeaderAccessor.SESSION_ID_HEADER, required = false) String sessionId
	) {
		validate(request);
		UUID userId = requireUser(principal, sessionId);
		requireMember(tripId, userId);
		if (!allow(sessionId)) {
			return;
		}
		messagingTemplate.convertAndSend(
			"/topic/trips/" + tripId + "/presence",
			new MapCursorEvent(
				"cursor.moved", tripId, userId, sessionId,
				request.longitude(), request.latitude(), request.sequence(), timeProvider.now()
			)
		);
	}

	/** 연결 종료 시 session별 throttle 상태를 제거한다. */
	@EventListener
	public void handleDisconnect(SessionDisconnectEvent event) {
		lastSentNanos.remove(event.getSessionId());
	}

	private boolean allow(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			throw new AccessDeniedException("Authenticated WebSocket session is required.");
		}
		long current = nanoTime.getAsLong();
		Long previous = lastSentNanos.putIfAbsent(sessionId, current);
		if (previous == null) {
			return true;
		}
		if (current - previous < MIN_INTERVAL_NANOS) {
			return false;
		}
		return lastSentNanos.replace(sessionId, previous, current);
	}

	private void validate(MapCursorRequest request) {
		if (request == null
			|| !Double.isFinite(request.longitude()) || request.longitude() < -180 || request.longitude() > 180
			|| !Double.isFinite(request.latitude()) || request.latitude() < -90 || request.latitude() > 90
			|| request.sequence() < 0) {
			throw new MessageConversionException("Map cursor payload is invalid.");
		}
	}

	private void requireMember(UUID tripId, UUID userId) {
		try {
			tripAccessGuard.requireActiveMember(tripId, userId);
		}
		catch (BusinessException exception) {
			throw new AccessDeniedException("Trip member cursor is required.", exception);
		}
	}

	private UUID requireUser(Principal principal, String sessionId) {
		if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
			try {
				return UUID.fromString(principal.getName());
			}
			catch (IllegalArgumentException exception) {
				throw new AccessDeniedException("Authenticated user ID must be a UUID.", exception);
			}
		}
		return sessionRegistry.findUserId(sessionId)
			.orElseThrow(() -> new AccessDeniedException("Authenticated WebSocket connection is required."));
	}
}
