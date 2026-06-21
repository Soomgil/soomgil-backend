package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * whitelist, confidence, serving discrimination으로 장소 확정 태그를 선택한다.
 *
 * <p>selection score는 {@code confidence * 0.50 + weight * 0.30 + discrimination * 0.20}이며,
 * 같은 태그는 최고 점수 후보만 사용하고 장소당 최대 개수를 넘기지 않는다.
 */
public class PlaceTagSelector {

	private static final BigDecimal CONFIDENCE_FACTOR = new BigDecimal("0.50");
	private static final BigDecimal WEIGHT_FACTOR = new BigDecimal("0.30");
	private static final BigDecimal DISCRIMINATION_FACTOR = new BigDecimal("0.20");
	private static final int SCORE_SCALE = 6;

	private final BigDecimal minimumConfidence;
	private final int maximumSelectedTags;

	public PlaceTagSelector(BigDecimal minimumConfidence, int maximumSelectedTags) {
		if (!isRate(minimumConfidence) || maximumSelectedTags < 1) {
			throw new IllegalArgumentException("tag selector configuration is invalid");
		}
		this.minimumConfidence = minimumConfidence;
		this.maximumSelectedTags = maximumSelectedTags;
	}

	public List<PlaceTagSelectionDecision> select(List<PlaceTagSelectionInput> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return List.of();
		}

		List<PlaceTagSelectionDecision> rejected = new ArrayList<>();
		List<PlaceTagSelectionDecision> eligible = new ArrayList<>();
		for (PlaceTagSelectionInput candidate : candidates) {
			if (candidate == null || candidate.tagId() == null || !candidate.activeSelectable()) {
				rejected.add(new PlaceTagSelectionDecision(candidate, null, "REJECTED_OUT_OF_DICTIONARY"));
				continue;
			}
			validateRates(candidate);
			BigDecimal score = selectionScore(candidate);
			if (candidate.confidence().compareTo(minimumConfidence) < 0) {
				rejected.add(new PlaceTagSelectionDecision(candidate, score, "REJECTED_LOW_CONFIDENCE"));
				continue;
			}
			eligible.add(new PlaceTagSelectionDecision(candidate, score, "SELECTED"));
		}

		eligible.sort(Comparator
			.comparing(PlaceTagSelectionDecision::selectionScore, Comparator.reverseOrder())
			.thenComparing(decision -> decision.input().confidence(), Comparator.reverseOrder())
			.thenComparing(decision -> decision.input().weight(), Comparator.reverseOrder())
			.thenComparing(decision -> decision.input().tagCode()));
		Set<String> selectedTagIds = new HashSet<>();
		List<PlaceTagSelectionDecision> ranked = new ArrayList<>();
		for (PlaceTagSelectionDecision decision : eligible) {
			String status;
			if (!selectedTagIds.add(decision.input().tagId())) {
				status = "REJECTED_DUPLICATE";
			} else if (selectedTagIds.size() > maximumSelectedTags) {
				status = "REJECTED_LIMIT";
			} else {
				status = "SELECTED";
			}
			ranked.add(new PlaceTagSelectionDecision(decision.input(), decision.selectionScore(), status));
		}
		ranked.addAll(rejected);
		return List.copyOf(ranked);
	}

	private BigDecimal selectionScore(PlaceTagSelectionInput candidate) {
		return candidate.confidence().multiply(CONFIDENCE_FACTOR)
			.add(candidate.weight().multiply(WEIGHT_FACTOR))
			.add(candidate.preferenceDiscrimination().multiply(DISCRIMINATION_FACTOR))
			.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
	}

	private void validateRates(PlaceTagSelectionInput candidate) {
		if (!isRate(candidate.confidence())
			|| !isRate(candidate.weight())
			|| !isRate(candidate.preferenceDiscrimination())) {
			throw new IllegalArgumentException("tag selection rates must be between 0 and 1");
		}
	}

	private boolean isRate(BigDecimal value) {
		return value != null
			&& value.compareTo(BigDecimal.ZERO) >= 0
			&& value.compareTo(BigDecimal.ONE) <= 0;
	}
}
