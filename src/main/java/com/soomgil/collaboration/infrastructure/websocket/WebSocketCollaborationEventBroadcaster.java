package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.collaboration.api.dto.CollaborationCommandEventMessage;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationEventBroadcaster;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 협업 command event를 trip별 STOMP topic으로 발행한다.
 */
@Component
public class WebSocketCollaborationEventBroadcaster implements CollaborationEventBroadcaster {

	private final SimpMessagingTemplate messagingTemplate;

	public WebSocketCollaborationEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
	}

	@Override
	public void broadcast(Long commandEventId, CollaborationCommandEvent event) {
		CollaborationCommandEventMessage message = toMessage(commandEventId, event);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send(event, message);
				}
			});
			return;
		}
		send(event, message);
	}

	private void send(CollaborationCommandEvent event, CollaborationCommandEventMessage message) {
		String base = "/topic/trips/" + event.tripId();
		messagingTemplate.convertAndSend(base + "/collaboration", message);
		String domainTopic = domainTopic(event.aggregateType());
		if (!"/collaboration".equals(domainTopic)) {
			messagingTemplate.convertAndSend(base + domainTopic, message);
		}
	}

	private String domainTopic(String aggregateType) {
		return switch (aggregateType) {
			case "ITINERARY", "ITINERARY_DAY", "ITINERARY_ITEM" -> "/itinerary";
			case "MAP_DRAWING" -> "/map-drawings";
			case "ROUTE_SEGMENT" -> "/route-matching";
			default -> "/collaboration";
		};
	}

	private CollaborationCommandEventMessage toMessage(Long commandEventId, CollaborationCommandEvent event) {
		return new CollaborationCommandEventMessage(
			commandEventId,
			event.tripId(),
			event.actorUserId(),
			event.websocketSessionId(),
			event.source(),
			event.commandType(),
			event.aggregateType(),
			event.aggregateId(),
			event.versionBefore(),
			event.versionAfter(),
			event.payload(),
			event.createdAt()
		);
	}
}
