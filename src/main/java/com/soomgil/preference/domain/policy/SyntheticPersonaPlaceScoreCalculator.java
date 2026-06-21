package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 페르소나 태그 성향과 장소 확정 태그를 결합해 signed 장소 점수를 계산한다.
 *
 * <p>계산식은 {@code 성향 강도 * confidence * weight}의 합이다. hard와 soft 강도는
 * 생성 정책 version에 따라 생성자에서 주입해 재현 가능하게 관리한다.
 */
public class SyntheticPersonaPlaceScoreCalculator {

	private static final int SCORE_SCALE = 6;

	private final BigDecimal hardPreferenceStrength;
	private final BigDecimal softPreferenceStrength;

	public SyntheticPersonaPlaceScoreCalculator(
		BigDecimal hardPreferenceStrength,
		BigDecimal softPreferenceStrength
	) {
		this.hardPreferenceStrength = requirePositive(hardPreferenceStrength, "hardPreferenceStrength");
		this.softPreferenceStrength = requirePositive(softPreferenceStrength, "softPreferenceStrength");
		if (hardPreferenceStrength.compareTo(softPreferenceStrength) <= 0) {
			throw new IllegalArgumentException("hard preference strength must exceed soft preference strength");
		}
	}

	/**
	 * 한 페르소나와 장소 태그의 점수를 계산한다.
	 *
	 * @param persona 검증을 통과한 페르소나 정의
	 * @param placeTags 장소의 확정 태그
	 * @return signed 점수와 hard 성향 일치 여부
	 */
	public SyntheticPersonaPlaceScore calculate(
		SyntheticPersonaDefinition persona,
		List<SyntheticPlaceTagInput> placeTags
	) {
		Objects.requireNonNull(persona, "persona must not be null");
		Objects.requireNonNull(placeTags, "placeTags must not be null");

		BigDecimal score = BigDecimal.ZERO;
		boolean hardLikeMatched = false;
		boolean hardDislikeMatched = false;
		for (SyntheticPlaceTagInput tag : placeTags) {
			validate(tag);
			BigDecimal evidence = tag.confidence().multiply(tag.weight());
			if (evidence.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			BigDecimal strength = preferenceStrength(persona, tag.tagCode());
			score = score.add(strength.multiply(evidence));
			hardLikeMatched |= persona.hardLikeTags().contains(tag.tagCode());
			hardDislikeMatched |= persona.hardDislikeTags().contains(tag.tagCode());
		}
		return new SyntheticPersonaPlaceScore(
			score.setScale(SCORE_SCALE, RoundingMode.HALF_UP),
			hardLikeMatched,
			hardDislikeMatched
		);
	}

	private BigDecimal preferenceStrength(SyntheticPersonaDefinition persona, String tagCode) {
		if (persona.hardLikeTags().contains(tagCode)) {
			return hardPreferenceStrength;
		}
		if (persona.hardDislikeTags().contains(tagCode)) {
			return hardPreferenceStrength.negate();
		}
		if (persona.softLikeTags().contains(tagCode)) {
			return softPreferenceStrength;
		}
		if (persona.softDislikeTags().contains(tagCode)) {
			return softPreferenceStrength.negate();
		}
		return BigDecimal.ZERO;
	}

	private void validate(SyntheticPlaceTagInput tag) {
		if (tag == null || tag.tagCode() == null || tag.tagCode().isBlank()) {
			throw new IllegalArgumentException("tagCode must not be blank");
		}
		if (tag.confidence() == null || tag.weight() == null
			|| tag.confidence().compareTo(BigDecimal.ZERO) < 0
			|| tag.weight().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("confidence and weight must not be negative");
		}
	}

	private BigDecimal requirePositive(BigDecimal value, String name) {
		if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}
}
