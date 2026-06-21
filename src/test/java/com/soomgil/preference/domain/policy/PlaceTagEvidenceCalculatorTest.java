package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceTagEvidenceCalculatorTest {

	private final PlaceTagEvidenceCalculator calculator = new PlaceTagEvidenceCalculator();

	@Test
	void distributesOnePlaceEvidenceAcrossConfirmedTags() {
		var evidence = calculator.calculate(List.of(
			new PlaceTagEvidenceInput("park", new BigDecimal("0.8000"), new BigDecimal("0.5000")),
			new PlaceTagEvidenceInput("museum", new BigDecimal("0.6000"), BigDecimal.ONE)
		));

		assertThat(evidence).hasSize(2);
		assertThat(evidence.get(0).tagId()).isEqualTo("park");
		assertThat(evidence.get(0).value()).isEqualByComparingTo("0.40000000");
		assertThat(evidence.get(1).tagId()).isEqualTo("museum");
		assertThat(evidence.get(1).value()).isEqualByComparingTo("0.60000000");
		assertThat(evidence.stream().map(PlaceTagEvidence::value).reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void rejectsTagsWhenTheirTotalEvidenceIsZero() {
		assertThatThrownBy(() -> calculator.calculate(List.of(
			new PlaceTagEvidenceInput("park", BigDecimal.ZERO, BigDecimal.ONE)
		)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("place tag evidence total must be greater than 0");
	}

	@Test
	void keepsTenRoundedTagValuesNonNegativeAndSummedToOne() {
		List<PlaceTagEvidenceInput> inputs = new ArrayList<>();
		for (int index = 1; index <= 9; index++) {
			inputs.add(new PlaceTagEvidenceInput("tag-" + index, BigDecimal.ONE, BigDecimal.ONE));
		}
		inputs.add(new PlaceTagEvidenceInput("tiny-tag", new BigDecimal("0.00000001"), BigDecimal.ONE));

		var evidence = calculator.calculate(inputs);

		assertThat(evidence).allSatisfy(item -> assertThat(item.value()).isNotNegative());
		assertThat(evidence.stream().map(PlaceTagEvidence::value).reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo(BigDecimal.ONE);
	}
}
