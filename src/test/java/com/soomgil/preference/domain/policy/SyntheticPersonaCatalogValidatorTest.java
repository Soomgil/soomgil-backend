package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SyntheticPersonaCatalogValidatorTest {

	private final SyntheticPersonaCatalogValidator validator = new SyntheticPersonaCatalogValidator(50);

	@Test
	void acceptsExactlyFiftyValidPersonas() {
		assertThatCode(() -> validator.validate(validPersonas(50)))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsCatalogWhenPersonaCountIsNotFifty() {
		assertThatThrownBy(() -> validator.validate(validPersonas(49)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("synthetic persona catalog must contain exactly 50 personas");
	}

	@Test
	void rejectsDuplicatePersonaKeys() {
		List<SyntheticPersonaDefinition> personas = validPersonas(50);
		personas.set(49, persona("persona-01", Set.of("quiet"), Set.of("lively"), "0.05"));

		assertThatThrownBy(() -> validator.validate(personas))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("synthetic persona keys must be unique");
	}

	@Test
	void rejectsTagThatIsBothHardLikeAndHardDislike() {
		List<SyntheticPersonaDefinition> personas = validPersonas(50);
		personas.set(0, persona("persona-01", Set.of("quiet"), Set.of("quiet"), "0.05"));

		assertThatThrownBy(() -> validator.validate(personas))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("hard like and hard dislike tags must not overlap");
	}

	@Test
	void rejectsNoiseRateAboveFivePercent() {
		List<SyntheticPersonaDefinition> personas = validPersonas(50);
		personas.set(0, persona("persona-01", Set.of("quiet"), Set.of("lively"), "0.051"));

		assertThatThrownBy(() -> validator.validate(personas))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("synthetic persona noise rate must be between 0 and 0.05");
	}

	private List<SyntheticPersonaDefinition> validPersonas(int count) {
		List<SyntheticPersonaDefinition> personas = new ArrayList<>();
		for (int index = 1; index <= count; index++) {
			personas.add(persona(
				"persona-%02d".formatted(index),
				Set.of("quiet"),
				Set.of("lively"),
				"0.05"
			));
		}
		return personas;
	}

	private SyntheticPersonaDefinition persona(
		String personaKey,
		Set<String> hardLikeTags,
		Set<String> hardDislikeTags,
		String noiseRate
	) {
		return new SyntheticPersonaDefinition(
			personaKey,
			"테스트 페르소나 " + personaKey,
			"고정된 테스트 성향",
			hardLikeTags,
			hardDislikeTags,
			Set.of("nature"),
			Set.of("urban"),
			Set.of("museum"),
			new BigDecimal(noiseRate),
			personaKey.hashCode()
		);
	}
}
