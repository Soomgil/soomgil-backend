package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 합성 반응 생성에 사용할 최신 성공 enrichment의 확정 태그 row.
 */
public record SyntheticPlaceTagSourceRow(
	String enrichmentId,
	String provider,
	String externalPlaceId,
	String tagCode,
	BigDecimal confidence,
	BigDecimal weight
) {
}
