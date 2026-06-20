package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SyntheticPersonaSwipeGeneratorTest {

	private final SyntheticPersonaSwipeGenerator generator = new SyntheticPersonaSwipeGenerator(
		new BigDecimal("1.20"),
		new BigDecimal("0.35"),
		new BigDecimal("-0.35")
	);

	@Test
	void returnsSuperLikeAtSuperLikeThreshold() {
		SyntheticSwipeReaction reaction = generate("1.20", false, false, 100L);

		assertThat(reaction).isEqualTo(SyntheticSwipeReaction.SUPER_LIKE);
	}

	@Test
	void returnsLikeAtLikeThreshold() {
		SyntheticSwipeReaction reaction = generate("0.35", false, false, 100L);

		assertThat(reaction).isEqualTo(SyntheticSwipeReaction.LIKE);
	}

	@Test
	void returnsNopeAtNopeThreshold() {
		SyntheticSwipeReaction reaction = generate("-0.35", false, false, 100L);

		assertThat(reaction).isEqualTo(SyntheticSwipeReaction.NOPE);
	}

	@Test
	void hardLikeNeverBecomesNope() {
		SyntheticSwipeReaction reaction = generate("-1.00", true, false, 100L);

		assertThat(reaction).isEqualTo(SyntheticSwipeReaction.LIKE);
	}

	@Test
	void hardDislikeAlwaysBecomesNope() {
		SyntheticSwipeReaction reaction = generate("2.00", false, true, 100L);

		assertThat(reaction).isEqualTo(SyntheticSwipeReaction.NOPE);
	}

	@Test
	void neutralScoreProducesSameReactionForSamePersonaPlaceAndSeed() {
		SyntheticSwipeReaction first = generate("0.10", false, false, 20260620L);
		SyntheticSwipeReaction second = generate("0.10", false, false, 20260620L);

		assertThat(second).isEqualTo(first);
		assertThat(first).isIn(SyntheticSwipeReaction.LIKE, SyntheticSwipeReaction.NOPE);
	}

	private SyntheticSwipeReaction generate(
		String score,
		boolean hardLikeMatched,
		boolean hardDislikeMatched,
		long seed
	) {
		return generator.generate(new SyntheticPersonaSwipeInput(
			"persona-01",
			"KTO",
			"126508",
			seed,
			new BigDecimal(score),
			hardLikeMatched,
			hardDislikeMatched
		));
	}
}
