package com.soomgil.trip.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

/**
 * trip.trip_regions 테이블 row.
 *
 * <p>법정동 상세 정보는 geo 모듈에서 관리하고, trip에서는 code와 정렬 순서만 저장한다.
 */
public record TripRegionRow(
	UUID tripId,
	String legalRegionCode,
	int sortOrder,
	Instant createdAt
) {
}
