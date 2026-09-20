package com.soomgil.voting.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soomgil.voting.api.dto.VoteSessionRealtimeEvent;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WebSocketVoteRealtimePublisherTest {

	@Test
	void publishesVoteSessionUpdateToTripTopic() {
		SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
		WebSocketVoteRealtimePublisher publisher = new WebSocketVoteRealtimePublisher(messagingTemplate);
		UUID tripId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();

		publisher.publish(tripId, sessionId, VoteSessionStatus.OPEN);

		ArgumentCaptor<VoteSessionRealtimeEvent> event = ArgumentCaptor.forClass(VoteSessionRealtimeEvent.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/trips/" + tripId + "/voting"), event.capture());
		org.assertj.core.api.Assertions.assertThat(event.getValue()).isEqualTo(
			new VoteSessionRealtimeEvent("vote.session.updated", tripId, sessionId, VoteSessionStatus.OPEN)
		);
	}
}
