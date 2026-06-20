package com.soomgil.preference.domain.policy;

/**
 * offline 추천 품질 평가용 순위 항목.
 *
 * @param itemId 평가 dataset 안에서 유일한 항목 ID
 * @param relevanceGrade {@code 0=무관, 1=긍정, 2=SUPER_LIKE}
 */
public record RecommendationEvaluationItem(
	String itemId,
	int relevanceGrade
) {
}
