package com.soomgil.collaboration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.api.dto.MapObjectTransformPreviewEvent;
import com.soomgil.collaboration.api.dto.MapObjectTransformPreviewRequest;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MapObjectTransformPreviewControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID DRAWING_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
	private static final UUID EDITOR_USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

	@Test
	void broadcastsAnotherMembersObjectTransformWhenEditorOwnsLease() {
		TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
		ItineraryCommandRepository repository = mock(ItineraryCommandRepository.class);
		when(repository.existsActiveMapDrawing(TRIP_ID, DRAWING_ID)).thenReturn(true);
		MapObjectLeaseStore leaseStore = mock(MapObjectLeaseStore.class);
		SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
		MapObjectTransformPreviewController controller = new MapObjectTransformPreviewController(
			tripAccessGuard, repository, leaseStore, messaging,
			new CollaborationWebSocketSessionRegistry(), () -> NOW
		);
		Map<String, Object> transform = Map.of(
			"centerLng", 127.1,
			"centerLat", 37.5,
			"widthMeters", 1200,
			"heightMeters", 800,
			"rotationDeg", 15
		);

		controller.preview(
			TRIP_ID,
			new MapObjectTransformPreviewRequest(DRAWING_ID, 7, "UPDATE", transform),
			() -> EDITOR_USER_ID.toString(),
			"editor-session"
		);

		verify(tripAccessGuard).requireActiveMember(TRIP_ID, EDITOR_USER_ID);
		verify(leaseStore).requireOwned(TRIP_ID, DRAWING_ID, EDITOR_USER_ID, "editor-session", NOW);
		ArgumentCaptor<MapObjectTransformPreviewEvent> event = ArgumentCaptor.forClass(
			MapObjectTransformPreviewEvent.class
		);
		verify(messaging).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			event.capture()
		);
		assertThat(event.getValue().clientId()).isEqualTo("editor-session");
		assertThat(event.getValue().transform()).isEqualTo(transform);
	}
}
