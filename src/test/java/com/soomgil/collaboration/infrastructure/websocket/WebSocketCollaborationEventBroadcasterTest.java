package com.soomgil.collaboration.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.soomgil.collaboration.api.dto.CollaborationCommandEventMessage;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class WebSocketCollaborationEventBroadcasterTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID AGGREGATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final WebSocketCollaborationEventBroadcaster broadcaster = new WebSocketCollaborationEventBroadcaster(messagingTemplate);

	@Test
	void broadcastsItineraryEventToCollaborationAndItineraryTopics() {
		CollaborationCommandEvent event = event("ITINERARY_ITEM");

		broadcaster.broadcast(9L, event);

		ArgumentCaptor<CollaborationCommandEventMessage> messageCaptor =
			ArgumentCaptor.forClass(CollaborationCommandEventMessage.class);
		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/collaboration"),
			messageCaptor.capture()
		);
		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/itinerary"),
			org.mockito.ArgumentMatchers.any(CollaborationCommandEventMessage.class)
		);
		org.assertj.core.api.Assertions.assertThat(messageCaptor.getValue().commandEventId()).isEqualTo(9L);
		org.assertj.core.api.Assertions.assertThat(messageCaptor.getValue().commandType()).isEqualTo("UPDATE_ITINERARY_ITEM");
	}

	@Test
	void broadcastsMapDrawingEventToMapDrawingTopic() {
		broadcaster.broadcast(10L, event("MAP_DRAWING"));

		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/map-drawings"),
			org.mockito.ArgumentMatchers.any(CollaborationCommandEventMessage.class)
		);
	}

	@Test
	void broadcastsRouteEventToRouteMatchingTopic() {
		broadcaster.broadcast(11L, event("ROUTE_SEGMENT"));

		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/route-matching"),
			org.mockito.ArgumentMatchers.any(CollaborationCommandEventMessage.class)
		);
	}

	@Test
	void broadcastsOnlyAfterTransactionCommit() {
		TransactionSynchronizationManager.initSynchronization();
		try {
			broadcaster.broadcast(12L, event("ITINERARY_ITEM"));
			verifyNoInteractions(messagingTemplate);

			TransactionSynchronizationManager.getSynchronizations().forEach(
				org.springframework.transaction.support.TransactionSynchronization::afterCommit);

			verify(messagingTemplate).convertAndSend(
				org.mockito.ArgumentMatchers.eq("/topic/trips/" + TRIP_ID + "/collaboration"),
				org.mockito.ArgumentMatchers.any(CollaborationCommandEventMessage.class)
			);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private CollaborationCommandEvent event(String aggregateType) {
		return new CollaborationCommandEvent(
			TRIP_ID,
			USER_ID,
			"session-1",
			"USER",
			"UPDATE_ITINERARY_ITEM",
			aggregateType,
			AGGREGATE_ID,
			1L,
			2L,
			"{\"id\":\"" + AGGREGATE_ID + "\"}",
			null,
			null,
			Instant.parse("2026-06-18T00:00:00Z")
		);
	}
}
