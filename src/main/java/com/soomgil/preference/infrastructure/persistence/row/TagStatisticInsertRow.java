package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 태그별 좋아요율과 호불호 점수 insert row.
 */
public record TagStatisticInsertRow(
	String runId,
	String tagId,
	BigDecimal preferenceDiscrimination,
	BigDecimal smoothedPositiveRate,
	long positiveCount,
	long reactionCount
) {
}
