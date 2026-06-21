package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 확정 태그의 {@code confidence * weight}를 한 장소 안에서 합계 1인 근거로 정규화한다.
 */
public class PlaceTagEvidenceCalculator {

	private static final int EVIDENCE_SCALE = 8;

	public List<PlaceTagEvidence> calculate(List<PlaceTagEvidenceInput> inputs) {
		if (inputs == null || inputs.isEmpty()) {
			return List.of();
		}

		List<BigDecimal> rawValues = inputs.stream()
			.map(this::rawEvidence)
			.toList();
		BigDecimal total = rawValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		if (total.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("place tag evidence total must be greater than 0");
		}

		List<BigDecimal> normalizedValues = new ArrayList<>(inputs.size());
		for (int index = 0; index < inputs.size(); index++) {
			normalizedValues.add(rawValues.get(index).divide(total, EVIDENCE_SCALE, RoundingMode.HALF_UP));
		}
		BigDecimal normalizedTotal = normalizedValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal roundingDifference = BigDecimal.ONE
			.setScale(EVIDENCE_SCALE, RoundingMode.HALF_UP)
			.subtract(normalizedTotal);
		int largestIndex = largestValueIndex(rawValues);
		normalizedValues.set(largestIndex, normalizedValues.get(largestIndex).add(roundingDifference));

		List<PlaceTagEvidence> result = new ArrayList<>(inputs.size());
		for (int index = 0; index < inputs.size(); index++) {
			result.add(new PlaceTagEvidence(inputs.get(index).tagId(), normalizedValues.get(index)));
		}
		return List.copyOf(result);
	}

	private int largestValueIndex(List<BigDecimal> values) {
		int largestIndex = 0;
		for (int index = 1; index < values.size(); index++) {
			if (values.get(index).compareTo(values.get(largestIndex)) > 0) {
				largestIndex = index;
			}
		}
		return largestIndex;
	}

	private BigDecimal rawEvidence(PlaceTagEvidenceInput input) {
		if (input == null || input.tagId() == null || input.tagId().isBlank()) {
			throw new IllegalArgumentException("tagId must not be blank");
		}
		if (input.confidence() == null || input.weight() == null) {
			throw new IllegalArgumentException("confidence and weight must not be null");
		}
		if (input.confidence().compareTo(BigDecimal.ZERO) < 0
			|| input.weight().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("confidence and weight must not be negative");
		}
		return input.confidence().multiply(input.weight());
	}
}
