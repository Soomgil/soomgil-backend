package com.soomgil.preference.infrastructure.persistence.row;

import java.time.OffsetDateTime;

/**
 * 장소 태깅 실행 기록 insert row.
 */
public record PlaceTagEnrichmentInsertRow(
	String id,
	String provider,
	String externalPlaceId,
	OffsetDateTime sourceModifiedAt,
	String sourceHash,
	String status,
	String modelProvider,
	String modelName,
	String promptVersion,
	String tagDictionaryVersion,
	String selectionPolicyVersion,
	String tagStatisticRunId,
	int candidateCount,
	int selectedCount
) {
}
