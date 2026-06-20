package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * source가 분리된 태그 통계 실행 insert row.
 */
public record TagStatisticRunInsertRow(
	String id,
	String source,
	BigDecimal alpha,
	BigDecimal globalPositiveRate,
	long totalReactionCount,
	long positiveReactionCount
) {
}
