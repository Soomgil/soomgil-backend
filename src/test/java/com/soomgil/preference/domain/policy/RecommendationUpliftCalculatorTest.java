package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecommendationUpliftCalculatorTest {

	private final RecommendationUpliftCalculator calculator = new RecommendationUpliftCalculator();

	@Test
	void calculatesMeasuredUpliftPercentAgainstBaseline() {
		BigDecimal uplift = calculator.calculatePercent(
			new BigDecimal("0.40"),
			new BigDecimal("0.50")
		);

		assertThat(uplift).isEqualByComparingTo("25.000000");
	}

	@Test
	void returnsZeroWhenBothMetricsAreZero() {
		assertThat(calculator.calculatePercent(BigDecimal.ZERO, BigDecimal.ZERO))
			.isEqualByComparingTo("0.000000");
	}

	@Test
	void rejectsUnmeasurablePercentWhenZeroBaselineImproves() {
		assertThatThrownBy(() -> calculator.calculatePercent(BigDecimal.ZERO, new BigDecimal("0.10")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("uplift percent is undefined when baseline is zero");
	}
}
