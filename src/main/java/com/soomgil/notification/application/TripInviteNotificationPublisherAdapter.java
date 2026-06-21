package com.soomgil.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.notification.api.dto.TripInviteNotificationPayload;
import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.trip.application.port.TripInviteNotificationPublisher;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 직접 여행방 초대를 수신자 소유의 인앱 알림으로 저장한다. */
@Component
public class TripInviteNotificationPublisherAdapter implements TripInviteNotificationPublisher {

	private final NotificationMapper mapper;
	private final ObjectMapper objectMapper;

	public TripInviteNotificationPublisherAdapter(NotificationMapper mapper, ObjectMapper objectMapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public void publish(
		UUID inviteId,
		UUID tripId,
		UUID actorUserId,
		UUID recipientUserId,
		String inviteCode,
		Instant createdAt
	) {
		String payload;
		try {
			payload = objectMapper.writeValueAsString(new TripInviteNotificationPayload(
				tripId, inviteId, inviteCode, "/trips/invites/" + inviteCode
			));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Trip invite notification payload could not be serialized.", exception);
		}
		mapper.insert(
			UUID.randomUUID(), recipientUserId, actorUserId, tripId, "TRIP_INVITE",
			"여행 초대가 도착했습니다.", null, payload, createdAt
		);
	}
}
