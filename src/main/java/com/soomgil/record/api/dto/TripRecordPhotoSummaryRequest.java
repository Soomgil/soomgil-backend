package com.soomgil.record.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 여러 여행의 기록 사진 요약을 한 번에 조회하는 요청.
 *
 * <p>{@code tripIds}는 1개 이상 100개 이하이며 null 요소를 허용하지 않는다.
 */
public record TripRecordPhotoSummaryRequest(
	@NotEmpty
	@Size(max = 100)
	List<@NotNull UUID> tripIds
) {
}
