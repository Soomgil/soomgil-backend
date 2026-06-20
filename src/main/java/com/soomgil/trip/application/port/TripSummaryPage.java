package com.soomgil.trip.application.port;

import java.util.List;

/**
 * 내 여행방 목록 조회 결과.
 *
 * <p>items는 현재 page 데이터만 담고, totalElements는 필터 적용 후 전체 개수다.
 */
public record TripSummaryPage(
	List<TripReadModel> items,
	long totalElements
) {

	public TripSummaryPage {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
