package com.soomgil.collaboration.api;

import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.global.error.BusinessException;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/** 지도 drawing preview STOMP 메시지를 같은 여행방 멤버에게 중계한다. */
@Controller
public class MapDrawingPreviewController {

	private final TripAccessGuard tripAccessGuard;
	private final SimpMessagingTemplate messagingTemplate;
	private final CollaborationWebSocketSessionRegistry sessionRegistry;

	public MapDrawingPreviewController(
		TripAccessGuard tripAccessGuard,
		SimpMessagingTemplate messagingTemplate,
		CollaborationWebSocketSessionRegistry sessionRegistry
	) {
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
	}

	@MessageMapping("/trips/{tripId}/map-drawing-preview")
	public void preview(
		@DestinationVariable UUID tripId,
		@Payload(required = false) Map<String, Object> payload,
		Principal principal,
		@Header(name = SimpMessageHeaderAccessor.SESSION_ID_HEADER, required = false) String sessionId
	) {
		UUID userId = requireUser(principal, sessionId);
		try {
			tripAccessGuard.requireActiveMember(tripId, userId);
		}
		catch (BusinessException exception) {
			throw new AccessDeniedException("Trip member preview is required.", exception);
		}
		Map<String, Object> message = new LinkedHashMap<>(payload == null ? Map.of() : payload);
		message.put("tripId", tripId.toString());
		messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/map-drawings", message);
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
