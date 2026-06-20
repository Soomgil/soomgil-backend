package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultSyntheticPersonaCatalogTest {

	private final DefaultSyntheticPersonaCatalog catalog = new DefaultSyntheticPersonaCatalog();

	@Test
	void providesExactlyFiftyValidPersonas() {
		List<SyntheticPersonaDefinition> personas = catalog.personas();

		assertThat(personas).hasSize(50);
		assertThatCode(() -> new SyntheticPersonaCatalogValidator(50).validate(personas))
			.doesNotThrowAnyException();
	}

	@Test
	void providesDiverseHardLikeTags() {
		long distinctHardLikeTagCount = catalog.personas().stream()
			.flatMap(persona -> persona.hardLikeTags().stream())
			.distinct()
			.count();

		assertThat(distinctHardLikeTagCount).isGreaterThanOrEqualTo(10);
	}

	@Test
	void returnsImmutableCatalog() {
		List<SyntheticPersonaDefinition> personas = catalog.personas();

		assertThat(personas).isUnmodifiable();
		assertThat(new HashSet<>(personas.stream().map(SyntheticPersonaDefinition::seed).toList()))
			.hasSize(50);
	}
}
