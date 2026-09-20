package com.soomgil.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.notification.api.dto.NotificationPayload;
import com.soomgil.notification.application.port.NotificationRealtimePublisher;
import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.social.application.port.SocialNotificationPublisher;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 팔로우 요청·팔로우·승인 알림을 사용자 알림함에 저장한다. */
@Component
public class SocialNotificationPublisherAdapter implements SocialNotificationPublisher {
	private final NotificationMapper mapper;
	private final ObjectMapper objectMapper;
	private final NotificationRealtimePublisher realtimePublisher;

	public SocialNotificationPublisherAdapter(NotificationMapper mapper, ObjectMapper objectMapper,
		NotificationRealtimePublisher realtimePublisher) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.realtimePublisher = Objects.requireNonNull(realtimePublisher, "realtimePublisher must not be null");
	}

	@Override
	public void publishFollowed(UUID followerUserId, UUID targetUserId, boolean approvalRequired, Instant createdAt) {
		insert(targetUserId, followerUserId, approvalRequired ? "FOLLOW_REQUESTED" : "FOLLOWED",
			approvalRequired ? "새 팔로우 요청이 도착했어요" : "새 팔로워가 생겼어요",
			approvalRequired ? "프로필에서 팔로우 요청을 확인해 주세요." : "새로운 여행 친구가 회원님을 팔로우합니다.",
			"/mypage/" + followerUserId, createdAt);
	}

	@Override
	public void publishAccepted(UUID acceptingUserId, UUID followerUserId, Instant createdAt) {
		insert(followerUserId, acceptingUserId, "FOLLOW_ACCEPTED", "팔로우 요청이 승인됐어요",
			"이제 공개된 여행 취향과 기록을 확인할 수 있어요.", "/mypage/" + acceptingUserId, createdAt);
	}

	private void insert(UUID recipientUserId, UUID actorUserId, String type, String title, String body,
		String route, Instant createdAt) {
		String payload;
		try {
			payload = objectMapper.writeValueAsString(new NotificationPayload(null, null, null, route, null));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Social notification payload could not be serialized.", exception);
		}
		mapper.insert(UUID.randomUUID(), recipientUserId, actorUserId, null, type, title, body, payload, createdAt);
		realtimePublisher.publishChanged(recipientUserId);
	}
}
