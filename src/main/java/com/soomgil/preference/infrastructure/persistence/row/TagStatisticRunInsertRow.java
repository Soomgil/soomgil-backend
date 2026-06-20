package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 실제 사용자 태그 통계 실행 insert row.
 */
public record TagStatisticRunInsertRow(
	String id,
	BigDecimal alpha,
	BigDecimal globalPositiveRate,
	long totalReactionCount,
	long positiveReactionCount
) {
}
