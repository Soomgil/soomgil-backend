package com.soomgil.notification.application.port;

import java.util.UUID;

/** 사용자 알림함 변경을 해당 사용자의 실시간 채널로 알린다. */
public interface NotificationRealtimePublisher {
	/** 수신자의 알림 목록과 읽지 않은 개수를 다시 동기화하도록 알린다. */
	void publishChanged(UUID recipientUserId);
}
