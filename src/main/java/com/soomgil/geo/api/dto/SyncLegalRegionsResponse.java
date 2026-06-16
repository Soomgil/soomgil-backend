package com.soomgil.geo.api.dto;

/**
 * 법정동 CSV 동기화 API 응답.
 *
 * <p>{@code totalCount}는 파일에서 실제 처리된 데이터 row 수이며 header는 제외한다.
 */
public record SyncLegalRegionsResponse(
	int totalCount,
	int insertedCount,
	int updatedCount,
	int deactivatedCount
) {
}
