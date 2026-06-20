package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 계산된 사용자 태그 선호도 update row.
 */
public record UserTagPreferenceScoreUpdateRow(
	String userId,
	String tagId,
	BigDecimal preferenceScore,
	String calculationVersion
) {
}
