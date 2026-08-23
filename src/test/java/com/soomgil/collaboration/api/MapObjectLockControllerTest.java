package com.soomgil.collaboration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soomgil.collaboration.api.dto.MapObjectLockAction;
import com.soomgil.collaboration.api.dto.MapObjectLockEvent;
import com.soomgil.collaboration.api.dto.MapObjectLockRequest;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.collaboration.infrastructure.websocket.InMemoryMapObjectLeaseStore;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MapObjectLockControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID DRAWING_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Test
	void acquiresLeaseAndBroadcastsServerOwnedLockState() {
		TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
		ItineraryCommandRepository repository = mock(ItineraryCommandRepository.class);
		org.mockito.Mockito.when(repository.existsActiveMapDrawing(TRIP_ID, DRAWING_ID)).thenReturn(true);
		SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
		CollaborationWebSocketSessionRegistry sessions = new CollaborationWebSocketSessionRegistry();
		sessions.register("session-1", USER_ID);
		MapObjectLockController controller = new MapObjectLockController(
			tripAccessGuard, repository, new InMemoryMapObjectLeaseStore(), messaging, sessions,
			() -> Instant.parse("2026-08-24T00:00:00Z")
		);

		controller.changeLock(
			TRIP_ID,
			new MapObjectLockRequest(DRAWING_ID, MapObjectLockAction.ACQUIRE),
			() -> USER_ID.toString(),
			"session-1"
		);

		ArgumentCaptor<MapObjectLockEvent> event = ArgumentCaptor.forClass(MapObjectLockEvent.class);
		verify(messaging).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			event.capture()
		);
		assertThat(event.getValue().locked()).isTrue();
		assertThat(event.getValue().userId()).isEqualTo(USER_ID);
		assertThat(event.getValue().clientId()).isEqualTo("session-1");
		assertThat(event.getValue().expiresAt()).isEqualTo(Instant.parse("2026-08-24T00:00:15Z"));
	}
}
