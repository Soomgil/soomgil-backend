package com.soomgil.collaboration.api;

import com.soomgil.collaboration.api.dto.MapObjectLockAction;
import com.soomgil.collaboration.api.dto.MapObjectLockEvent;
import com.soomgil.collaboration.api.dto.MapObjectLockRequest;
import com.soomgil.collaboration.application.port.MapObjectLease;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.security.Principal;
import java.time.Instant;
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

/** 지도 오브젝트의 15초 편집 lease를 변경하고 같은 여행방에 상태를 전파한다. */
@Controller
public class MapObjectLockController {

	private final TripAccessGuard tripAccessGuard;
	private final ItineraryCommandRepository itineraryRepository;
	private final MapObjectLeaseStore leaseStore;
	private final SimpMessagingTemplate messagingTemplate;
	private final CollaborationWebSocketSessionRegistry sessionRegistry;
	private final TimeProvider timeProvider;

	public MapObjectLockController(
		TripAccessGuard tripAccessGuard,
		ItineraryCommandRepository itineraryRepository,
		MapObjectLeaseStore leaseStore,
		SimpMessagingTemplate messagingTemplate,
		CollaborationWebSocketSessionRegistry sessionRegistry,
		TimeProvider timeProvider
	) {
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.itineraryRepository = Objects.requireNonNull(itineraryRepository, "itineraryRepository must not be null");
		this.leaseStore = Objects.requireNonNull(leaseStore, "leaseStore must not be null");
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@MessageMapping("/trips/{tripId}/map-object-lock")
	public void changeLock(
		@DestinationVariable UUID tripId,
		@Payload MapObjectLockRequest request,
		Principal principal,
		@Header(name = SimpMessageHeaderAccessor.SESSION_ID_HEADER, required = false) String sessionId
	) {
		if (request == null || request.drawingId() == null || request.action() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Map object lock fields are required.");
		}
		UUID userId = requireUser(principal, sessionId);
		requireMember(tripId, userId);
		if (!itineraryRepository.existsActiveMapDrawing(tripId, request.drawingId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
		}
		Instant now = timeProvider.now();
		MapObjectLockEvent event;
		if (request.action() == MapObjectLockAction.RELEASE) {
			if (!leaseStore.release(tripId, request.drawingId(), userId, sessionId)) {
				throw new BusinessException(ErrorCode.CONFLICT, "Owned map object lease was not found.");
			}
			event = MapObjectLockEvent.released(tripId, request.drawingId());
		}
		else {
			MapObjectLease lease = request.action() == MapObjectLockAction.ACQUIRE
				? leaseStore.acquire(tripId, request.drawingId(), userId, sessionId, now)
				: leaseStore.renew(tripId, request.drawingId(), userId, sessionId, now);
			event = MapObjectLockEvent.locked(
				tripId, request.drawingId(), userId, lease.websocketSessionId(), lease.expiresAt()
			);
		}
		messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/map-drawings", event);
	}

	private void requireMember(UUID tripId, UUID userId) {
		try {
			tripAccessGuard.requireActiveMember(tripId, userId);
		}
		catch (BusinessException exception) {
			throw new AccessDeniedException("Trip member lock is required.", exception);
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
