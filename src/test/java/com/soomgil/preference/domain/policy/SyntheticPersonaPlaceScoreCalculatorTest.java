package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SyntheticPersonaPlaceScoreCalculatorTest {

	private final SyntheticPersonaPlaceScoreCalculator calculator =
		new SyntheticPersonaPlaceScoreCalculator(
			new BigDecimal("1.50"),
			new BigDecimal("0.60")
		);

	@Test
	void calculatesSignedScoreFromPersonaPreferenceAndPlaceEvidence() {
		SyntheticPersonaDefinition persona = persona(
			Set.of("quiet"),
			Set.of("lively"),
			Set.of("nature"),
			Set.of("urban")
		);

		SyntheticPersonaPlaceScore result = calculator.calculate(persona, List.of(
			tag("quiet", "0.8", "1.0"),
			tag("nature", "0.5", "1.0"),
			tag("urban", "0.2", "1.0")
		));

		assertThat(result.score()).isEqualByComparingTo("1.380000");
		assertThat(result.hardLikeMatched()).isTrue();
		assertThat(result.hardDislikeMatched()).isFalse();
	}

	@Test
	void hardDislikeUsesNegativeStrengthAndMarksConstraint() {
		SyntheticPersonaDefinition persona = persona(
			Set.of("quiet"),
			Set.of("lively"),
			Set.of(),
			Set.of()
		);

		SyntheticPersonaPlaceScore result = calculator.calculate(persona, List.of(
			tag("lively", "1.0", "1.0")
		));

		assertThat(result.score()).isEqualByComparingTo("-1.500000");
		assertThat(result.hardDislikeMatched()).isTrue();
	}

	@Test
	void ignoresTagsNotDefinedByPersona() {
		SyntheticPersonaPlaceScore result = calculator.calculate(
			persona(Set.of("quiet"), Set.of(), Set.of(), Set.of()),
			List.of(tag("museum", "1.0", "1.0"))
		);

		assertThat(result.score()).isEqualByComparingTo("0.000000");
	}

	private SyntheticPersonaDefinition persona(
		Set<String> hardLikes,
		Set<String> hardDislikes,
		Set<String> softLikes,
		Set<String> softDislikes
	) {
		return new SyntheticPersonaDefinition(
			"persona-01",
			"테스트 페르소나",
			"점수 계산 테스트",
			hardLikes,
			hardDislikes,
			softLikes,
			softDislikes,
			Set.of(),
			new BigDecimal("0.05"),
			100L
		);
	}

	private SyntheticPlaceTagInput tag(String tagCode, String confidence, String weight) {
		return new SyntheticPlaceTagInput(
			tagCode,
			new BigDecimal(confidence),
			new BigDecimal(weight)
		);
	}
}
