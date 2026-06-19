package com.soomgil.trip.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

/**
 * trip.trip_members 테이블 row.
 *
 * <p>MVP에서는 owner도 role 값으로는 MEMBER만 저장한다.
 */
public record TripMemberRow(
	UUID id,
	UUID tripId,
	UUID userId,
	String role,
	String status,
	Instant joinedAt,
	Instant leftAt,
	UUID removedByUserId
) {
}
