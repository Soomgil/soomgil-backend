package com.soomgil.trip.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

/**
 * trip.trip_invites 테이블 row.
 *
 * <p>inviteTokenHash는 저장소 전용 값이며 API 응답에 노출하지 않는다.
 */
public record TripInviteRow(
	UUID id,
	UUID tripId,
	UUID createdByUserId,
	UUID inviteeUserId,
	String inviteCode,
	String inviteTokenHash,
	String status,
	Instant expiresAt,
	UUID acceptedByUserId,
	Instant acceptedAt,
	Instant revokedAt,
	Instant createdAt
) {
}
