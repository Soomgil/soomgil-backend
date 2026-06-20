package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SyntheticPersonaNoisePolicyTest {

	private final SyntheticPersonaNoisePolicy policy = new SyntheticPersonaNoisePolicy();

	@Test
	void neverChangesHardPreferenceScore() {
		BigDecimal result = policy.apply(input("place-1", "0.05", true));

		assertThat(result).isEqualByComparingTo("0.600000");
	}

	@Test
	void returnsSameResultForSamePersonaPlaceAndSeed() {
		BigDecimal first = policy.apply(input("place-1", "0.05", false));
		BigDecimal second = policy.apply(input("place-1", "0.05", false));

		assertThat(second).isEqualByComparingTo(first);
	}

	@Test
	void appliesNoiseToNoMoreThanFivePercentOfSoftScores() {
		int changed = 0;
		for (int index = 0; index < 10_000; index++) {
			BigDecimal result = policy.apply(input("place-" + index, "0.05", false));
			if (result.signum() < 0) {
				changed++;
			}
		}

		assertThat(changed).isPositive().isLessThanOrEqualTo(550);
	}

	@Test
	void rejectsNoiseRateAboveFivePercent() {
		assertThatThrownBy(() -> policy.apply(input("place-1", "0.051", false)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("noise rate must be between 0 and 0.05");
	}

	private SyntheticPersonaNoiseInput input(String placeId, String noiseRate, boolean hardPreferenceMatched) {
		return new SyntheticPersonaNoiseInput(
			"persona-01",
			"KTO",
			placeId,
			100L,
			new BigDecimal(noiseRate),
			new BigDecimal("0.600000"),
			hardPreferenceMatched
		);
	}
}
