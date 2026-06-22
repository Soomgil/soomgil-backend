package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PreferenceDiscriminationCalculatorTest {

	private final PreferenceDiscriminationCalculator calculator = new PreferenceDiscriminationCalculator();

	@Test
	void calculatesSmoothedPositiveRateAndDistanceFromGlobalRate() {
		TagPreferenceStatistics statistics = calculator.calculate(
			80,
			100,
			new BigDecimal("0.600000"),
			new BigDecimal("100")
		);

		assertThat(statistics.smoothedPositiveRate()).isEqualByComparingTo("0.700000");
		assertThat(statistics.preferenceDiscrimination()).isEqualByComparingTo("0.166667");
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
		assertThat(smallSample.preferenceDiscrimination()).isEqualByComparingTo("0.030303");
		assertThat(smallSample.preferenceDiscrimination())
			.isLessThan(largeSample.preferenceDiscrimination());
	}

	@Test
	void measuresDiscriminationWithoutTreatingTheGlobalRateAsPreferenceDirection() {
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
		assertThat(balanced.preferenceDiscrimination()).isEqualByComparingTo("0.000000");
		assertThat(oneSided.smoothedPositiveRate()).isEqualByComparingTo("0.800000");
		assertThat(oneSided.preferenceDiscrimination()).isEqualByComparingTo("0.333333");
		assertThat(oneSided.preferenceDiscrimination())
			.isGreaterThan(balanced.preferenceDiscrimination());
	}

	@Test
	void keepsPreferenceDiscriminationWithinZeroAndOne() {
		TagPreferenceStatistics allPositive = calculator.calculate(
			100,
			100,
			BigDecimal.ZERO,
			new BigDecimal("100")
		);
		TagPreferenceStatistics allNegative = calculator.calculate(
			0,
			100,
			BigDecimal.ONE,
			new BigDecimal("100")
		);

		assertThat(allPositive.preferenceDiscrimination()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
		assertThat(allNegative.preferenceDiscrimination()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
	}
}
