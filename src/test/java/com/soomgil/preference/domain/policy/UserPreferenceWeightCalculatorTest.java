package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UserPreferenceWeightCalculatorTest {

	private final UserPreferenceWeightCalculator calculator = new UserPreferenceWeightCalculator();

	@Test
	void calculatesTagImportanceFromPreferenceDiscrimination() {
		assertThat(calculator.calculateTagImportance(BigDecimal.ZERO))
			.isEqualByComparingTo("0.700000");
		assertThat(calculator.calculateTagImportance(new BigDecimal("0.5")))
			.isEqualByComparingTo("0.850000");
		assertThat(calculator.calculateTagImportance(BigDecimal.ONE))
			.isEqualByComparingTo("1.000000");
	}
}
