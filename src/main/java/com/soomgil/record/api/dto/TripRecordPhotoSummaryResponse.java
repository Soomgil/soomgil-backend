package com.soomgil.record.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 요청 순서대로 반환되는 여행별 기록 사진 요약 목록.
 */
public record TripRecordPhotoSummaryResponse(
	@NotNull
	List<@Valid TripRecordPhotoSummary> items
) {
}
