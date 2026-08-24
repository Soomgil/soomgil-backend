package com.soomgil.collaboration.api;

import com.soomgil.collaboration.api.dto.MapObjectTransformPreviewEvent;
import com.soomgil.collaboration.api.dto.MapObjectTransformPreviewRequest;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/** 활성 lease 소유자의 지도 오브젝트 transform preview를 같은 여행방에 중계한다. */
@Controller
public class MapObjectTransformPreviewController {

	private final TripAccessGuard tripAccessGuard;
	private final ItineraryCommandRepository itineraryRepository;
	private final MapObjectLeaseStore leaseStore;
	private final SimpMessagingTemplate messagingTemplate;
	private final CollaborationWebSocketSessionRegistry sessionRegistry;
	private final TimeProvider timeProvider;

	public MapObjectTransformPreviewController(
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

	@MessageMapping("/trips/{tripId}/map-object-transform-preview")
	public void preview(
		@DestinationVariable UUID tripId,
		@Payload(required = false) MapObjectTransformPreviewRequest request,
		Principal principal,
		@Header(name = SimpMessageHeaderAccessor.SESSION_ID_HEADER, required = false) String sessionId
	) {
		validate(request, sessionId);
		UUID userId = requireUser(principal, sessionId);
		requireMember(tripId, userId);
		if (!itineraryRepository.existsActiveMapDrawing(tripId, request.drawingId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
		}
		leaseStore.requireOwned(tripId, request.drawingId(), userId, sessionId, timeProvider.now());
		messagingTemplate.convertAndSend(
			"/topic/trips/" + tripId + "/map-drawings",
			MapObjectTransformPreviewEvent.of(tripId, userId, sessionId, request, timeProvider.now())
		);
	}

	private void validate(MapObjectTransformPreviewRequest request, String sessionId) {
		if (sessionId == null || sessionId.isBlank() || request == null || request.drawingId() == null
			|| request.sequence() < 0
			|| !("UPDATE".equals(request.phase()) || "END".equals(request.phase()) || "CANCEL".equals(request.phase()))
			|| !validTransform(request.transform())) {
			throw new MessageConversionException("Map object transform preview payload is invalid.");
		}
	}

	private boolean validTransform(Map<String, Object> transform) {
		if (transform == null) return false;
		return finiteBetween(transform.get("centerLng"), -180D, 180D)
			&& finiteBetween(transform.get("centerLat"), -90D, 90D)
			&& positive(transform.get("widthMeters"))
			&& positive(transform.get("heightMeters"))
			&& finiteBetween(transform.get("rotationDeg"), -36000D, 36000D);
	}

	private boolean finiteBetween(Object value, double minimum, double maximum) {
		return value instanceof Number number && Double.isFinite(number.doubleValue())
			&& number.doubleValue() >= minimum && number.doubleValue() <= maximum;
	}

	private boolean positive(Object value) {
		return value instanceof Number number && Double.isFinite(number.doubleValue())
			&& number.doubleValue() > 0D && number.doubleValue() <= 20_000_000D;
	}

	private void requireMember(UUID tripId, UUID userId) {
		try {
			tripAccessGuard.requireActiveMember(tripId, userId);
		}
		catch (BusinessException exception) {
			throw new AccessDeniedException("Trip member preview is required.", exception);
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
