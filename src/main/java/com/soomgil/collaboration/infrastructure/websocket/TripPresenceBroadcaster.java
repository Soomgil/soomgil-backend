package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.collaboration.api.dto.TripPresenceEvent;
import com.soomgil.collaboration.api.dto.MapObjectLockEvent;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class TripPresenceBroadcaster {

	private final CollaborationWebSocketSessionRegistry sessionRegistry;
	private final ObjectFactory<SimpMessagingTemplate> messagingTemplateFactory;
	private final MapObjectLeaseStore leaseStore;

	@Autowired
	public TripPresenceBroadcaster(
		CollaborationWebSocketSessionRegistry sessionRegistry,
		ObjectFactory<SimpMessagingTemplate> messagingTemplateFactory,
		MapObjectLeaseStore leaseStore
	) {
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
		this.messagingTemplateFactory = Objects.requireNonNull(
			messagingTemplateFactory,
			"messagingTemplateFactory must not be null"
		);
		this.leaseStore = Objects.requireNonNull(leaseStore, "leaseStore must not be null");
	}

	public TripPresenceBroadcaster(
		CollaborationWebSocketSessionRegistry sessionRegistry,
		ObjectFactory<SimpMessagingTemplate> messagingTemplateFactory
	) {
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
		this.messagingTemplateFactory = Objects.requireNonNull(
			messagingTemplateFactory,
			"messagingTemplateFactory must not be null"
		);
		this.leaseStore = null;
	}

	public void registerSubscription(String sessionId, UUID userId, UUID tripId) {
		if (sessionId == null || userId == null || tripId == null) {
			return;
		}
		sessionRegistry.registerTripPresence(sessionId, tripId, userId);
		broadcastSnapshot(tripId, sessionRegistry.activeUserIds(tripId));
	}

	public void unregisterSession(String sessionId) {
		if (leaseStore != null) {
			leaseStore.releaseSession(sessionId).forEach(lease ->
				messagingTemplateFactory.getObject().convertAndSend(
					"/topic/trips/" + lease.tripId() + "/map-drawings",
					MapObjectLockEvent.released(lease.tripId(), lease.drawingId())
				)
			);
		}
		broadcastSnapshots(sessionRegistry.unregister(sessionId));
	}

	@EventListener
	public void handleDisconnect(SessionDisconnectEvent event) {
		unregisterSession(event.getSessionId());
	}

	private void broadcastSnapshots(Map<UUID, List<UUID>> snapshots) {
		snapshots.forEach(this::broadcastSnapshot);
	}

	private void broadcastSnapshot(UUID tripId, List<UUID> activeUserIds) {
		messagingTemplateFactory.getObject().convertAndSend(
			"/topic/trips/" + tripId + "/presence",
			TripPresenceEvent.snapshot(tripId, activeUserIds)
		);
	}
}
