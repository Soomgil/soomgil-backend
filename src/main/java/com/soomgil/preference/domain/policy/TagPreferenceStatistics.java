package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 태그 반응에서 계산한 보정 좋아요율과 신뢰도 반영 호불호 점수.
 *
 * @param smoothedPositiveRate 전체 좋아요율과 사전 반응 수로 보정한 태그 좋아요율
 * @param preferenceDiscrimination 표본 신뢰도를 반영한 호불호 균형 점수
 */
public record TagPreferenceStatistics(
	BigDecimal smoothedPositiveRate,
	BigDecimal preferenceDiscrimination
) {
}
