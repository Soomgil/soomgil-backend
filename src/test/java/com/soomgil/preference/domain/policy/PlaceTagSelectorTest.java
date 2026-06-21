package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceTagSelectorTest {

	private final PlaceTagSelector selector = new PlaceTagSelector(
		new BigDecimal("0.55"),
		10
	);

	@Test
	void rejectsUnknownInactiveAndLowConfidenceCandidates() {
		List<PlaceTagSelectionDecision> decisions = selector.select(List.of(
			input(null, "unknown", true, "0.99", "1.00", "1.00"),
			input("inactive-id", "inactive", false, "0.99", "1.00", "1.00"),
			input("low-id", "low", true, "0.54", "1.00", "1.00")
		));

		assertThat(decisions).extracting(PlaceTagSelectionDecision::status)
			.containsExactly(
				"REJECTED_OUT_OF_DICTIONARY",
				"REJECTED_OUT_OF_DICTIONARY",
				"REJECTED_LOW_CONFIDENCE"
			);
	}

	@Test
	void calculatesSelectionScoreAndSortsSelectedTags() {
		List<PlaceTagSelectionDecision> decisions = selector.select(List.of(
			input("park-id", "park", true, "0.80", "0.50", "0.50"),
			input("quiet-id", "quiet", true, "0.90", "0.70", "0.80")
		));

		assertThat(decisions).extracting(decision -> decision.input().tagCode())
			.containsExactly("quiet", "park");
		assertThat(decisions).extracting(PlaceTagSelectionDecision::selectionScore)
			.containsExactly(new BigDecimal("0.820000"), new BigDecimal("0.650000"));
		assertThat(decisions).extracting(PlaceTagSelectionDecision::status)
			.containsOnly("SELECTED");
	}

	@Test
	void selectsOnlyBestDuplicateAndAtMostTenTags() {
		List<PlaceTagSelectionInput> candidates = new ArrayList<>();
		candidates.add(input("same-id", "duplicate-low", true, "0.60", "0.50", "0.50"));
		candidates.add(input("same-id", "duplicate-high", true, "0.95", "0.90", "0.50"));
		for (int index = 0; index < 10; index++) {
			candidates.add(input(
				"tag-" + index,
				"tag-" + index,
				true,
				"0.80",
				"0.70",
				"0.50"
			));
		}

		List<PlaceTagSelectionDecision> decisions = selector.select(candidates);

		assertThat(decisions).filteredOn(PlaceTagSelectionDecision::selected).hasSize(10);
		assertThat(decisions).filteredOn(decision -> decision.input().tagCode().equals("duplicate-high"))
			.extracting(PlaceTagSelectionDecision::status)
			.containsExactly("SELECTED");
		assertThat(decisions).filteredOn(decision -> decision.input().tagCode().equals("duplicate-low"))
			.extracting(PlaceTagSelectionDecision::status)
			.containsExactly("REJECTED_DUPLICATE");
	}

	private PlaceTagSelectionInput input(
		String tagId,
		String tagCode,
		boolean activeSelectable,
		String confidence,
		String weight,
		String discrimination
	) {
		return new PlaceTagSelectionInput(
			tagId,
			tagCode,
			activeSelectable,
			new BigDecimal(confidence),
			new BigDecimal(weight),
			new BigDecimal(discrimination),
			"run-id"
		);
	}
}
