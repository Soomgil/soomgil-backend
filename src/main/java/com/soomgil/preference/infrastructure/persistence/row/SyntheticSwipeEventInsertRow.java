package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 실제 사용자 이벤트와 분리해 저장하는 합성 스와이프 event row.
 */
public record SyntheticSwipeEventInsertRow(
	String personaId,
	String provider,
	String externalPlaceId,
	String reaction,
	String enrichmentId,
	String generatorVersion,
	long seed,
	BigDecimal personaPlaceScore
) {
}
