package com.soomgil.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.notification.api.dto.TripInviteNotificationPayload;
import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.notification.infrastructure.persistence.TripInviteEmailRecipientMapper;
import com.soomgil.notification.application.port.NotificationRealtimePublisher;
import com.soomgil.auth.application.service.MailService;
import com.soomgil.trip.application.port.TripInviteNotificationPublisher;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 직접 여행방 초대를 수신자 소유의 인앱 알림으로 저장한다. */
@Component
public class TripInviteNotificationPublisherAdapter implements TripInviteNotificationPublisher {
	private static final Logger log = LoggerFactory.getLogger(TripInviteNotificationPublisherAdapter.class);

	private final NotificationMapper mapper;
	private final ObjectMapper objectMapper;
	private final TripInviteEmailRecipientMapper emailRecipientMapper;
	private final MailService mailService;
	private final NotificationRealtimePublisher realtimePublisher;

	public TripInviteNotificationPublisherAdapter(NotificationMapper mapper, ObjectMapper objectMapper,
		TripInviteEmailRecipientMapper emailRecipientMapper, MailService mailService,
		NotificationRealtimePublisher realtimePublisher) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.emailRecipientMapper = Objects.requireNonNull(emailRecipientMapper, "emailRecipientMapper must not be null");
		this.mailService = Objects.requireNonNull(mailService, "mailService must not be null");
		this.realtimePublisher = Objects.requireNonNull(realtimePublisher, "realtimePublisher must not be null");
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
				tripId, inviteId, inviteCode, "/trip-invites/" + inviteCode
			));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Trip invite notification payload could not be serialized.", exception);
		}
		mapper.insert(
			UUID.randomUUID(), recipientUserId, actorUserId, tripId, "TRIP_INVITE",
			"여행 초대가 도착했습니다.", null, payload, createdAt
		);
		realtimePublisher.publishChanged(recipientUserId);

		String recipientEmail = emailRecipientMapper.findOptedInVerifiedEmail(recipientUserId);
		if (recipientEmail != null) {
			try {
				mailService.sendTripInviteEmail(recipientEmail, inviteCode);
			}
			catch (RuntimeException exception) {
				// 선택 채널인 이메일 실패가 초대 생성과 인앱 알림을 되돌리지 않도록 격리한다.
				log.warn("Trip invite email could not be sent to user {}", recipientUserId, exception);
			}
		}
	}
}
