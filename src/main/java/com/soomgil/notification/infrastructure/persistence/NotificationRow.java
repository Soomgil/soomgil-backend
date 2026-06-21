package com.soomgil.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/** 알림과 선택적 actor 공개 정보를 합친 읽기 row. */
public record NotificationRow(
	UUID id,
	UUID actorUserId,
	String actorDisplayName,
	String actorProfileImageUrl,
	String type,
	String title,
	String body,
	String payloadJson,
	Instant readAt,
	Instant createdAt
) {
}
