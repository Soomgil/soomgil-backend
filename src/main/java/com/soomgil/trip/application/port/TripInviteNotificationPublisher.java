package com.soomgil.trip.application.port;

import java.time.Instant;
import java.util.UUID;

/** Trip 초대 생성 결과를 notification 모듈에 전달하는 application port. */
public interface TripInviteNotificationPublisher {

	void publish(
		UUID inviteId,
		UUID tripId,
		UUID actorUserId,
		UUID recipientUserId,
		String inviteCode,
		Instant createdAt
	);
}
