package com.soomgil.planning.infrastructure.websocket;

import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Planning 변경 이벤트를 여행방별 STOMP topic으로 commit 이후 발행한다. */
@Component
public class WebSocketPlanningEventBroadcaster implements PlanningEventBroadcaster {

	private final SimpMessagingTemplate messagingTemplate;

	public WebSocketPlanningEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
	}

	@Override
	public void broadcast(PlanningRealtimeEvent event) {
		Objects.requireNonNull(event, "event must not be null");
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

	private void send(PlanningRealtimeEvent event) {
		messagingTemplate.convertAndSend("/topic/trips/" + event.tripId() + "/planning", event);
	}
}
