package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 장소 확정 태그 근거 계산에 사용하는 persistence row.
 */
public record PlaceTagEvidenceSourceRow(
	String enrichmentId,
	String tagId,
	BigDecimal confidence,
	BigDecimal weight
) {
}
