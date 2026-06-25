package com.soomgil.trip.infrastructure.persistence.row;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * trip.trips 테이블 row.
 *
 * <p>DB enum은 문자열로 저장하므로 row에서도 문자열을 유지하고 repository에서
 * domain enum으로 변환한다.
 */
public record TripRow(
	UUID id,
	UUID ownerUserId,
	String title,
	String displayDestination,
	String status,
	long itineraryVersion,
	LocalDate startDate,
	LocalDate endDate,
	String coverImageUrl,
	UUID retrippedFromPostId,
	Integer retrippedFromSnapshotVersion,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt
) {
}
