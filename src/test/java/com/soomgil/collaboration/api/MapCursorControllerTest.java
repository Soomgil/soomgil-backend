package com.soomgil.collaboration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.api.dto.MapCursorEvent;
import com.soomgil.collaboration.api.dto.MapCursorRequest;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class MapCursorControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Test
	void broadcastsAuthenticatedCursorWithServerSessionIdentity() {
		TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
		SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
		CollaborationWebSocketSessionRegistry sessions = new CollaborationWebSocketSessionRegistry();
		sessions.register("session-1", USER_ID);
		AtomicLong nanos = new AtomicLong(1_000_000_000L);
		MapCursorController controller = new MapCursorController(
			tripAccessGuard, messaging, sessions,
			() -> Instant.parse("2026-08-24T00:00:00Z"), nanos::get
		);

		controller.cursor(
			TRIP_ID, new MapCursorRequest(126.5312, 33.4996, 7), () -> USER_ID.toString(), "session-1"
		);

		ArgumentCaptor<MapCursorEvent> event = ArgumentCaptor.forClass(MapCursorEvent.class);
		verify(messaging).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/presence"),
			event.capture()
		);
		assertThat(event.getValue().userId()).isEqualTo(USER_ID);
		assertThat(event.getValue().clientId()).isEqualTo("session-1");
	}

	@Test
	void rejectsOutOfRangeCoordinates() {
		MapCursorController controller = new MapCursorController(
			mock(TripAccessGuard.class), mock(SimpMessagingTemplate.class),
			new CollaborationWebSocketSessionRegistry(),
			() -> Instant.parse("2026-08-24T00:00:00Z"), () -> 0L
		);

		assertThatThrownBy(() -> controller.cursor(
			TRIP_ID, new MapCursorRequest(181, 33, 1), () -> USER_ID.toString(), "session-1"
		)).isInstanceOf(MessageConversionException.class);
	}

	@Test
	void throttlesCursorToTwentyHertzAndResetsOnDisconnect() {
		SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
		AtomicLong nanos = new AtomicLong(1_000_000_000L);
		MapCursorController controller = new MapCursorController(
			mock(TripAccessGuard.class), messaging, new CollaborationWebSocketSessionRegistry(),
			() -> Instant.parse("2026-08-24T00:00:00Z"), nanos::get
		);

		MapCursorRequest request = new MapCursorRequest(126.5312, 33.4996, 1);
		controller.cursor(TRIP_ID, request, () -> USER_ID.toString(), "session-1");
		controller.cursor(TRIP_ID, request, () -> USER_ID.toString(), "session-1");
		verify(messaging, times(1)).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/presence"),
			org.mockito.ArgumentMatchers.any(MapCursorEvent.class)
		);

		SessionDisconnectEvent disconnectEvent = mock(SessionDisconnectEvent.class);
		when(disconnectEvent.getSessionId()).thenReturn("session-1");
		controller.handleDisconnect(disconnectEvent);
		controller.cursor(TRIP_ID, request, () -> USER_ID.toString(), "session-1");
		verify(messaging, times(2)).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/presence"),
			org.mockito.ArgumentMatchers.any(MapCursorEvent.class)
		);
	}
}
