package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 동일한 {@code K}에서 측정한 추천 품질 metric 집합.
 */
public record RecommendationQualityMetrics(
	BigDecimal precisionAtK,
	BigDecimal recallAtK,
	BigDecimal ndcgAtK,
	BigDecimal hitRateAtK,
	BigDecimal superLikeHitRateAtK
) {
}
