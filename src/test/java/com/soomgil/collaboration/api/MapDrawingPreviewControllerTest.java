package com.soomgil.collaboration.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class MapDrawingPreviewControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	private final TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final CollaborationWebSocketSessionRegistry sessionRegistry = new CollaborationWebSocketSessionRegistry();
	private final TimeProvider timeProvider = () -> Instant.parse("2026-08-24T00:00:00Z");
	private final MapDrawingPreviewController controller = new MapDrawingPreviewController(
		tripAccessGuard,
		messagingTemplate,
		sessionRegistry,
		timeProvider
	);

	@Test
	void broadcastsPreviewToTripMapDrawingTopic() {
		Map<String, Object> payload = new java.util.HashMap<>(validPayload());
		payload.putAll(Map.of(
			"tripId", "malicious-trip",
			"clientId", "client-1"
		));

		controller.preview(TRIP_ID, payload, () -> USER_ID.toString(), "session-1");

		ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
		verify(tripAccessGuard).requireActiveMember(TRIP_ID, USER_ID);
		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			messageCaptor.capture()
		);
		org.assertj.core.api.Assertions.assertThat(messageCaptor.getValue())
			.containsEntry("tripId", TRIP_ID.toString())
			.containsEntry("clientId", "session-1")
			.containsEntry("previewId", "preview-1")
			.containsEntry("userId", USER_ID.toString())
			.containsEntry("sentAt", "2026-08-24T00:00:00Z");
	}

	@Test
	void broadcastsPreviewFromRegisteredSessionWhenPrincipalIsMissing() {
		sessionRegistry.register("session-1", USER_ID);

		controller.preview(TRIP_ID, validPayload(), null, "session-1");

		verify(tripAccessGuard).requireActiveMember(TRIP_ID, USER_ID);
		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			org.mockito.ArgumentMatchers.any(Map.class)
		);
	}

	@Test
	void rejectsNonMemberPreview() {
		org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN))
			.when(tripAccessGuard)
			.requireActiveMember(TRIP_ID, USER_ID);

		assertThatThrownBy(() -> controller.preview(TRIP_ID, validPayload(), () -> USER_ID.toString(), "session-1"))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void rejectsUnauthenticatedPreview() {
		assertThatThrownBy(() -> controller.preview(TRIP_ID, validPayload(), null, "session-1"))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void rejectsOversizedCoordinatePayload() {
		Map<String, Object> payload = new java.util.HashMap<>(validPayload());
		payload.put("coordinates", java.util.stream.IntStream.range(0, 33)
			.mapToObj(index -> Map.of("lng", 126.0, "lat", 33.0))
			.toList());

		assertThatThrownBy(() -> controller.preview(TRIP_ID, payload, () -> USER_ID.toString(), "session-1"))
			.isInstanceOf(MessageConversionException.class);
	}

	@Test
	void throttlesPreviewToTwentyHertzAndResetsOnDisconnect() {
		AtomicLong nanos = new AtomicLong(1_000_000_000L);
		MapDrawingPreviewController throttledController = new MapDrawingPreviewController(
			tripAccessGuard, messagingTemplate, sessionRegistry, timeProvider, nanos::get
		);

		throttledController.preview(TRIP_ID, validPayload(), () -> USER_ID.toString(), "session-1");
		throttledController.preview(TRIP_ID, validPayload(), () -> USER_ID.toString(), "session-1");
		verify(messagingTemplate, times(1)).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			org.mockito.ArgumentMatchers.any(Map.class)
		);

		SessionDisconnectEvent disconnectEvent = mock(SessionDisconnectEvent.class);
		when(disconnectEvent.getSessionId()).thenReturn("session-1");
		throttledController.handleDisconnect(disconnectEvent);
		throttledController.preview(TRIP_ID, validPayload(), () -> USER_ID.toString(), "session-1");
		verify(messagingTemplate, times(2)).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			org.mockito.ArgumentMatchers.any(Map.class)
		);
	}

	private Map<String, Object> validPayload() {
		return Map.of(
			"previewId", "preview-1",
			"sequence", 1,
			"phase", "UPDATE",
			"coordinates", List.of(Map.of("lng", 126.0, "lat", 33.0)),
			"color", "#111827",
			"width", 4
		);
	}
}
