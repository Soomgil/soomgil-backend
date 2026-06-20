package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 사용자 태그 선호도 계산에 필요한 근거와 serving 태그 통계 row.
 */
public record UserTagPreferenceScoreSourceRow(
	String tagId,
	BigDecimal positiveEvidence,
	BigDecimal negativeEvidence,
	BigDecimal smoothedPositiveRate,
	BigDecimal preferenceDiscrimination
) {
}
