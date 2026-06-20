package com.soomgil.record.application.port;

import java.util.List;

/**
 * 여행 기록 page 조회 결과.
 */
public record TripRecordPage(
	List<TripRecordEntryReadModel> items,
	long totalElements
) {
}
