package com.soomgil.planning.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WebSocketPlanningEventBroadcasterTest {

	@Test
	void broadcastsToTheTripPlanningTopic() {
		SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
		WebSocketPlanningEventBroadcaster broadcaster = new WebSocketPlanningEventBroadcaster(messagingTemplate);
		PlanningRealtimeEvent event = mock(PlanningRealtimeEvent.class);
		UUID tripId = UUID.randomUUID();
		org.mockito.Mockito.when(event.tripId()).thenReturn(tripId);

		broadcaster.broadcast(event);

		verify(messagingTemplate).convertAndSend("/topic/trips/" + tripId + "/planning", event);
	}
}
