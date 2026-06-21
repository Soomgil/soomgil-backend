package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UserPreferenceScoreCalculatorTest {

	private final UserPreferenceScoreCalculator calculator = new UserPreferenceScoreCalculator();

	@Test
	void returnsTheTagLikeRateBeforeTheUserHasEvidence() {
		BigDecimal score = calculator.calculate(
			new BigDecimal("0.600000"),
			new BigDecimal("0.800000"),
			BigDecimal.ZERO,
			BigDecimal.ZERO
		);

		assertThat(score).isEqualByComparingTo("0.600000");
	}

	@Test
	void trustsPersonalEvidenceFasterForPolarizingTags() {
		BigDecimal polarizingTagScore = calculator.calculate(
			new BigDecimal("0.600000"),
			new BigDecimal("0.800000"),
			BigDecimal.ZERO,
			BigDecimal.ONE
		);
		BigDecimal consensusTagScore = calculator.calculate(
			new BigDecimal("0.600000"),
			new BigDecimal("0.200000"),
			BigDecimal.ZERO,
			BigDecimal.ONE
		);

		assertThat(polarizingTagScore).isEqualByComparingTo("0.120000");
		assertThat(consensusTagScore).isEqualByComparingTo("0.480000");
		assertThat(polarizingTagScore).isLessThan(consensusTagScore);
	}

	@Test
	void keepsConsensusOnlyTagsAtTheGlobalLikeRate() {
		BigDecimal score = calculator.calculate(
			new BigDecimal("0.700000"),
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			new BigDecimal("100")
		);

		assertThat(score).isEqualByComparingTo("0.700000");
	}

	@Test
	void letsPersonalEvidenceFullyDecideMaximallyPolarizingTags() {
		BigDecimal positiveScore = calculator.calculate(
			new BigDecimal("0.600000"),
			BigDecimal.ONE,
			BigDecimal.ONE,
			BigDecimal.ZERO
		);
		BigDecimal negativeScore = calculator.calculate(
			new BigDecimal("0.600000"),
			BigDecimal.ONE,
			BigDecimal.ZERO,
			BigDecimal.ONE
		);

		assertThat(positiveScore).isEqualByComparingTo("1.000000");
		assertThat(negativeScore).isEqualByComparingTo("0.000000");
	}
}
