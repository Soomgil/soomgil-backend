package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 장소 태깅 후보 insert row.
 */
public record PlaceTagEnrichmentCandidateInsertRow(
	String id,
	String enrichmentId,
	String candidateCode,
	String matchedTagId,
	BigDecimal confidence,
	BigDecimal weight,
	BigDecimal selectionScore,
	String status,
	String rationale
) {
}
