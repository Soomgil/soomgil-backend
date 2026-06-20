package com.soomgil.geo.application.command.dto;

/**
 * 법정동 CSV 동기화 결과.
 */
public record SyncLegalRegionsResult(
	int totalCount,
	int insertedCount,
	int updatedCount,
	int deactivatedCount
) {
}
