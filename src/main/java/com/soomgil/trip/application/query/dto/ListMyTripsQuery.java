package com.soomgil.trip.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.List;
import java.util.UUID;

/**
 * 현재 사용자가 active member인 여행방 목록을 조회하는 query.
 *
 * <p>page는 0부터 시작하고, size는 handler에서 허용 범위로 검증한다.
 */
public record ListMyTripsQuery(
	UUID userId,
	TripStatus status,
	TripAccessRole role,
	int page,
	int size,
	List<String> sort
) implements Query<PagedTripSummaryView> {

	public ListMyTripsQuery {
		sort = sort == null ? List.of() : List.copyOf(sort);
	}
}
