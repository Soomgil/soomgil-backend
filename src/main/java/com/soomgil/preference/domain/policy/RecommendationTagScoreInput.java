package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 사용자 태그 선호도와 장소 내 정규화 태그 근거를 결합하는 추천 점수 입력.
 *
 * @param preferenceScore 사용자의 태그 선호도, {@code 0..1}
 * @param normalizedPlaceTagEvidence 장소 내 태그 근거 비율, 같은 장소에서 합계 1
 */
public record RecommendationTagScoreInput(
	BigDecimal preferenceScore,
	BigDecimal normalizedPlaceTagEvidence
) {
}
