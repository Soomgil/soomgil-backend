package com.soomgil.voting.infrastructure.websocket;

import com.soomgil.voting.api.dto.VoteSessionRealtimeEvent;
import com.soomgil.voting.application.port.VoteRealtimePublisher;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 투표 상태 변경을 transaction commit 이후 여행방별 STOMP topic으로 발행한다. */
@Component
public class WebSocketVoteRealtimePublisher implements VoteRealtimePublisher {

	private static final String EVENT_TYPE = "vote.session.updated";

	private final SimpMessagingTemplate messagingTemplate;

	public WebSocketVoteRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
	}

	@Override
	public void publish(UUID tripId, UUID sessionId, VoteSessionStatus status) {
		VoteSessionRealtimeEvent event = new VoteSessionRealtimeEvent(EVENT_TYPE, tripId, sessionId, status);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send(event);
				}
			});
			return;
		}
		send(event);
	}

	private void send(VoteSessionRealtimeEvent event) {
		messagingTemplate.convertAndSend("/topic/trips/" + event.tripId() + "/voting", event);
	}
}
