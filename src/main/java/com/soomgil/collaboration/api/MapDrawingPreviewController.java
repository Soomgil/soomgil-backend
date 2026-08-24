package com.soomgil.collaboration.api;

import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.security.Principal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** 지도 drawing preview STOMP 메시지를 같은 여행방 멤버에게 중계한다. */
@Controller
public class MapDrawingPreviewController {

	private final TripAccessGuard tripAccessGuard;
	private final SimpMessagingTemplate messagingTemplate;
	private final CollaborationWebSocketSessionRegistry sessionRegistry;
	private final TimeProvider timeProvider;
	private final LongSupplier nanoTime;
	private final ConcurrentHashMap<String, Long> lastSentNanos = new ConcurrentHashMap<>();
	private static final long MIN_INTERVAL_NANOS = 50_000_000L;

	@Autowired
	public MapDrawingPreviewController(
		TripAccessGuard tripAccessGuard,
		SimpMessagingTemplate messagingTemplate,
		CollaborationWebSocketSessionRegistry sessionRegistry,
		TimeProvider timeProvider
	) {
		this(tripAccessGuard, messagingTemplate, sessionRegistry, timeProvider, System::nanoTime);
	}

	MapDrawingPreviewController(
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

	/** 연결 종료 시 session별 throttle 상태를 제거한다. */
	@EventListener
	public void handleDisconnect(SessionDisconnectEvent event) {
		lastSentNanos.remove(event.getSessionId());
	}

	@MessageMapping("/trips/{tripId}/map-drawing-preview")
	public void preview(
		@DestinationVariable UUID tripId,
		@Payload(required = false) Map<String, Object> payload,
		Principal principal,
		@Header(name = SimpMessageHeaderAccessor.SESSION_ID_HEADER, required = false) String sessionId
	) {
		validate(payload, sessionId);
		UUID userId = requireUser(principal, sessionId);
		try {
			tripAccessGuard.requireActiveMember(tripId, userId);
		}
		catch (BusinessException exception) {
			throw new AccessDeniedException("Trip member preview is required.", exception);
		}
		if ("UPDATE".equals(payload.get("phase")) && !allow(sessionId)) {
			return;
		}
		Map<String, Object> message = new LinkedHashMap<>(payload);
		message.put("tripId", tripId.toString());
		message.put("clientId", sessionId);
		message.put("userId", userId.toString());
		message.put("sentAt", timeProvider.now().toString());
		messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/map-drawings", message);
	}

	private void validate(Map<String, Object> payload, String sessionId) {
		if (sessionId == null || sessionId.isBlank() || payload == null
			|| !(payload.get("previewId") instanceof String previewId) || previewId.isBlank() || previewId.length() > 120
			|| !(payload.get("sequence") instanceof Number sequence) || sequence.longValue() < 0
			|| !(payload.get("phase") instanceof String phase)
			|| !(phase.equals("UPDATE") || phase.equals("END") || phase.equals("CANCEL"))
			|| !(payload.get("coordinates") instanceof List<?> coordinates) || coordinates.size() > 100
			|| !(payload.get("color") instanceof String color) || !color.matches("#[0-9a-fA-F]{6}")
			|| !(payload.get("width") instanceof Number width) || !Double.isFinite(width.doubleValue())
			|| width.doubleValue() <= 0 || width.doubleValue() > 40
			|| !coordinates.stream().allMatch(this::validCoordinate)) {
			throw new MessageConversionException("Map drawing preview payload is invalid.");
		}
	}

	private boolean validCoordinate(Object value) {
		if (!(value instanceof Map<?, ?> coordinate)
			|| !(coordinate.get("lng") instanceof Number longitude)
			|| !(coordinate.get("lat") instanceof Number latitude)) {
			return false;
		}
		return Double.isFinite(longitude.doubleValue()) && longitude.doubleValue() >= -180 && longitude.doubleValue() <= 180
			&& Double.isFinite(latitude.doubleValue()) && latitude.doubleValue() >= -90 && latitude.doubleValue() <= 90;
	}

	private boolean allow(String sessionId) {
		long now = nanoTime.getAsLong();
		Long previous = lastSentNanos.putIfAbsent(sessionId, now);
		if (previous == null) return true;
		if (now - previous < MIN_INTERVAL_NANOS) return false;
		return lastSentNanos.replace(sessionId, previous, now);
	}

	private UUID requireUser(Principal principal, String sessionId) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return sessionRegistry.findUserId(sessionId)
				.orElseThrow(() -> new AccessDeniedException("Authenticated WebSocket connection is required."));
		}
		try {
			return UUID.fromString(principal.getName());
		}
		catch (IllegalArgumentException exception) {
			throw new AccessDeniedException("Authenticated user ID must be a UUID.", exception);
		}
	}
}
