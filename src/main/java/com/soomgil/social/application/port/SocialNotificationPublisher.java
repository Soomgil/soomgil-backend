package com.soomgil.social.application.port;

import java.time.Instant;
import java.util.UUID;

/** 팔로우 관계 변경 알림을 저장하고 수신자에게 전달한다. */
public interface SocialNotificationPublisher {
	void publishFollowed(UUID followerUserId, UUID targetUserId, boolean approvalRequired, Instant createdAt);
	void publishAccepted(UUID acceptingUserId, UUID followerUserId, Instant createdAt);
}
