package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 확정 장소 태그 insert row.
 */
public record PlaceTagEnrichmentTagInsertRow(
	String enrichmentId,
	String tagId,
	BigDecimal confidence,
	BigDecimal weight,
	BigDecimal preferenceDiscriminationSnapshot,
	BigDecimal selectionScore,
	int rankOrder,
	String tagStatisticRunId,
	String rationale
) {
}
