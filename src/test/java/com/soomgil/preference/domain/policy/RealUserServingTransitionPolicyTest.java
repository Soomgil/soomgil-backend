package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RealUserServingTransitionPolicyTest {

	private final RealUserServingTransitionPolicy policy =
		new RealUserServingTransitionPolicy(10_000, 100);

	@Test
	void allowsAutomaticTransitionWhenTotalAndEveryCoreTagAreStable() {
		boolean allowed = policy.canPromote(10_000, List.of(100L, 150L, 200L), false);

		assertThat(allowed).isTrue();
	}

	@Test
	void blocksAutomaticTransitionWhenTotalReactionCountIsTooSmall() {
		boolean allowed = policy.canPromote(9_999, List.of(100L, 150L), false);

		assertThat(allowed).isFalse();
	}

	@Test
	void blocksAutomaticTransitionWhenAnyCoreTagIsUnstable() {
		boolean allowed = policy.canPromote(10_000, List.of(100L, 99L), false);

		assertThat(allowed).isFalse();
	}

	@Test
	void allowsExplicitTransitionAfterOfflineEvaluationApproval() {
		boolean allowed = policy.canPromote(10, List.of(1L), true);

		assertThat(allowed).isTrue();
	}
}
