package com.soomgil.notification.infrastructure.websocket;

import com.soomgil.notification.api.dto.NotificationRealtimeEvent;
import com.soomgil.notification.application.port.NotificationRealtimePublisher;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 알림함 변경을 transaction commit 이후 사용자별 STOMP queue로 발행한다. */
@Component
public class WebSocketNotificationRealtimePublisher implements NotificationRealtimePublisher {
	private static final NotificationRealtimeEvent EVENT = new NotificationRealtimeEvent("notification.changed");
	private final SimpMessagingTemplate messagingTemplate;

	public WebSocketNotificationRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
	}

	@Override
	public void publishChanged(UUID recipientUserId) {
		Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send(recipientUserId);
				}
			});
			return;
		}
		send(recipientUserId);
	}

	private void send(UUID recipientUserId) {
		messagingTemplate.convertAndSendToUser(recipientUserId.toString(), "/queue/notifications", EVENT);
	}
}
