package com.soomgil.trip.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 멤버 목록 조회 row.
 *
 * <p>ownerUserId는 저장된 role과 별도로 OWNER access role을 파생하기 위해 포함한다.
 */
public record TripMemberReadRow(
	UUID id,
	UUID tripId,
	UUID userId,
	String role,
	String status,
	Instant joinedAt,
	UUID ownerUserId
) {
}
