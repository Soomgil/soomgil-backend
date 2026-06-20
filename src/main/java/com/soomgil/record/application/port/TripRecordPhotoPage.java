package com.soomgil.record.application.port;

import java.util.List;

/**
 * 여행 기록 사진 page 조회 결과.
 */
public record TripRecordPhotoPage(
	List<TripRecordPhotoReadModel> items,
	long totalElements
) {
}
