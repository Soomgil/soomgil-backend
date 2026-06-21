package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationQualityEvaluatorTest {

	private final RecommendationQualityEvaluator evaluator = new RecommendationQualityEvaluator();

	@Test
	void calculatesRankingMetricsAtK() {
		RecommendationQualityMetrics metrics = evaluator.evaluate(List.of(
			new RecommendationEvaluationItem("super", 2),
			new RecommendationEvaluationItem("relevant", 1),
			new RecommendationEvaluationItem("irrelevant", 0)
		), 2);

		assertThat(metrics.precisionAtK()).isEqualByComparingTo("1.000000");
		assertThat(metrics.recallAtK()).isEqualByComparingTo("1.000000");
		assertThat(metrics.ndcgAtK()).isEqualByComparingTo("1.000000");
		assertThat(metrics.hitRateAtK()).isEqualByComparingTo("1.000000");
		assertThat(metrics.superLikeHitRateAtK()).isEqualByComparingTo("1.000000");
	}

	@Test
	void penalizesRelevantAndSuperLikeItemsRankedBelowK() {
		RecommendationQualityMetrics metrics = evaluator.evaluate(List.of(
			new RecommendationEvaluationItem("irrelevant", 0),
			new RecommendationEvaluationItem("relevant", 1),
			new RecommendationEvaluationItem("super", 2)
		), 2);

		assertThat(metrics.precisionAtK()).isEqualByComparingTo("0.500000");
		assertThat(metrics.recallAtK()).isEqualByComparingTo("0.500000");
		assertThat(metrics.ndcgAtK()).isBetween(
			new BigDecimal("0.173700"),
			new BigDecimal("0.173800")
		);
		assertThat(metrics.hitRateAtK()).isEqualByComparingTo("1.000000");
		assertThat(metrics.superLikeHitRateAtK()).isEqualByComparingTo("0.000000");
	}
}
