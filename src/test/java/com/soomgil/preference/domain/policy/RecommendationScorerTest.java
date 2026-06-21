package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationScorerTest {

	private final RecommendationScorer scorer = new RecommendationScorer(new BigDecimal("0.15"));

	@Test
	void calculatesMemberPlaceScoreAsAWeightedAverageOfTagPreferences() {
		BigDecimal score = scorer.calculateMemberScore(List.of(
			new RecommendationTagScoreInput(new BigDecimal("0.800000"), new BigDecimal("0.60000000")),
			new RecommendationTagScoreInput(new BigDecimal("0.300000"), new BigDecimal("0.40000000"))
		));

		assertThat(score).isEqualByComparingTo("0.600000");
	}

	@Test
	void sumsActiveMemberScoresForTheGroup() {
		BigDecimal groupScore = scorer.calculateGroupScore(List.of(
			new BigDecimal("0.600000"),
			new BigDecimal("0.400000"),
			new BigDecimal("0.750000")
		));

		assertThat(groupScore).isEqualByComparingTo("1.750000");
	}

	@Test
	void matchesMembersOnlyWhenTheirScoreIsAboveNeutralByTheThreshold() {
		assertThat(scorer.isMatchedMember(new BigDecimal("0.649999"))).isFalse();
		assertThat(scorer.isMatchedMember(new BigDecimal("0.650000"))).isTrue();
	}

	@Test
	void rejectsPlaceTagEvidenceThatDoesNotSumToOne() {
		assertThatThrownBy(() -> scorer.calculateMemberScore(List.of(
			new RecommendationTagScoreInput(new BigDecimal("0.800000"), new BigDecimal("0.40000000")),
			new RecommendationTagScoreInput(new BigDecimal("0.300000"), new BigDecimal("0.40000000"))
		)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("normalized place tag evidence must sum to 1");
	}
}
