package com.soomgil.trip.application.query.dto;

import java.util.List;

/**
 * 내 여행방 목록 page view.
 *
 * <p>API 계층은 이 view를 {@code PagedTripSummary}로 변환한다.
 */
public record PagedTripSummaryView(
	List<TripSummaryView> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	List<String> sort
) {

	public PagedTripSummaryView {
		items = items == null ? List.of() : List.copyOf(items);
		sort = sort == null ? List.of() : List.copyOf(sort);
	}
}
