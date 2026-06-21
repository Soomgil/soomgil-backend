package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 순위가 있는 offline dataset에서 추천 품질 metric을 계산한다.
 *
 * <p>입력 순서가 추천 순위다. relevance grade {@code 1}은 긍정 반응,
 * {@code 2}는 SUPER_LIKE이며 NDCG gain은 {@code 2^grade - 1}을 사용한다.
 */
public class RecommendationQualityEvaluator {

	private static final int METRIC_SCALE = 6;

	public RecommendationQualityMetrics evaluate(List<RecommendationEvaluationItem> rankedItems, int k) {
		validate(rankedItems, k);
		int topSize = Math.min(k, rankedItems.size());
		List<RecommendationEvaluationItem> top = rankedItems.subList(0, topSize);
		long totalRelevant = rankedItems.stream().filter(this::relevant).count();
		long topRelevant = top.stream().filter(this::relevant).count();
		boolean superLikeHit = top.stream().anyMatch(item -> item.relevanceGrade() == 2);

		BigDecimal precision = ratio(topRelevant, k);
		BigDecimal recall = totalRelevant == 0 ? zero() : ratio(topRelevant, totalRelevant);
		BigDecimal ndcg = ndcgAtK(rankedItems, k);
		BigDecimal hitRate = topRelevant > 0 ? one() : zero();
		BigDecimal superLikeHitRate = superLikeHit ? one() : zero();
		return new RecommendationQualityMetrics(precision, recall, ndcg, hitRate, superLikeHitRate);
	}

	private BigDecimal ndcgAtK(List<RecommendationEvaluationItem> rankedItems, int k) {
		double dcg = dcg(rankedItems.stream().map(RecommendationEvaluationItem::relevanceGrade).toList(), k);
		List<Integer> idealGrades = rankedItems.stream()
			.map(RecommendationEvaluationItem::relevanceGrade)
			.sorted((first, second) -> Integer.compare(second, first))
			.toList();
		double idealDcg = dcg(idealGrades, k);
		if (idealDcg == 0.0) {
			return zero();
		}
		return BigDecimal.valueOf(dcg / idealDcg).setScale(METRIC_SCALE, RoundingMode.HALF_UP);
	}

	private double dcg(List<Integer> grades, int k) {
		double result = 0.0;
		for (int index = 0; index < Math.min(k, grades.size()); index++) {
			double gain = Math.pow(2.0, grades.get(index)) - 1.0;
			result += gain / (Math.log(index + 2.0) / Math.log(2.0));
		}
		return result;
	}

	private BigDecimal ratio(long numerator, long denominator) {
		return BigDecimal.valueOf(numerator)
			.divide(BigDecimal.valueOf(denominator), METRIC_SCALE, RoundingMode.HALF_UP);
	}

	private boolean relevant(RecommendationEvaluationItem item) {
		return item.relevanceGrade() > 0;
	}

	private void validate(List<RecommendationEvaluationItem> rankedItems, int k) {
		if (rankedItems == null || k < 1) {
			throw new IllegalArgumentException("ranked items must exist and K must be positive");
		}
		Set<String> itemIds = new HashSet<>();
		for (RecommendationEvaluationItem item : rankedItems) {
			if (item == null || item.itemId() == null || item.itemId().isBlank()
				|| !itemIds.add(item.itemId())
				|| item.relevanceGrade() < 0
				|| item.relevanceGrade() > 2) {
				throw new IllegalArgumentException("evaluation items must be unique with grade between 0 and 2");
			}
		}
	}

	private BigDecimal zero() {
		return BigDecimal.ZERO.setScale(METRIC_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal one() {
		return BigDecimal.ONE.setScale(METRIC_SCALE, RoundingMode.HALF_UP);
	}
}
