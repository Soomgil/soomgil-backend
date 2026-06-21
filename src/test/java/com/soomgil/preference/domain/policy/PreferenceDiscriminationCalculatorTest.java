package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PreferenceDiscriminationCalculatorTest {

	private final PreferenceDiscriminationCalculator calculator = new PreferenceDiscriminationCalculator();

	@Test
	void calculatesSmoothedLikeRateAndReliablePolarization() {
		TagPreferenceStatistics statistics = calculator.calculate(
			80,
			100,
			new BigDecimal("0.600000"),
			new BigDecimal("100")
		);

		assertThat(statistics.smoothedPositiveRate()).isEqualByComparingTo("0.700000");
		assertThat(statistics.preferenceDiscrimination()).isEqualByComparingTo("0.420000");
	}

	@Test
	void shrinksSmallSamplesTowardTheGlobalLikeRate() {
		TagPreferenceStatistics smallSample = calculator.calculate(
			8,
			10,
			new BigDecimal("0.600000"),
			new BigDecimal("100")
		);
		TagPreferenceStatistics largeSample = calculator.calculate(
			80,
			100,
			new BigDecimal("0.600000"),
			new BigDecimal("100")
		);

		assertThat(smallSample.smoothedPositiveRate()).isEqualByComparingTo("0.618182");
		assertThat(smallSample.preferenceDiscrimination()).isEqualByComparingTo("0.085830");
		assertThat(smallSample.preferenceDiscrimination())
			.isLessThan(largeSample.preferenceDiscrimination());
	}

	@Test
	void givesMoreWeightToBalancedReactionsThanOneSidedReactions() {
		TagPreferenceStatistics balanced = calculator.calculate(
			60,
			100,
			new BigDecimal("0.600000"),
			new BigDecimal("100")
		);
		TagPreferenceStatistics oneSided = calculator.calculate(
			100,
			100,
			new BigDecimal("0.600000"),
			new BigDecimal("100")
		);

		assertThat(balanced.smoothedPositiveRate()).isEqualByComparingTo("0.600000");
		assertThat(balanced.preferenceDiscrimination()).isEqualByComparingTo("0.480000");
		assertThat(oneSided.smoothedPositiveRate()).isEqualByComparingTo("0.800000");
		assertThat(oneSided.preferenceDiscrimination()).isEqualByComparingTo("0.320000");
		assertThat(balanced.preferenceDiscrimination())
			.isGreaterThan(oneSided.preferenceDiscrimination());
	}
}
