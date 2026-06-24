package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 멤버별 장소 점수, 그룹 합의 점수, matched member 여부를 계산한다.
 *
 * <p>멤버별 점수는 태그 선호도의 가중평균이라 {@code 0..1} 범위다.
 * matched threshold는 멤버별 장소 점수에 직접 적용한다.
 */
public class RecommendationScorer {

	private static final int SCORE_SCALE = 6;

	private final BigDecimal matchedMemberThreshold;

	public RecommendationScorer(BigDecimal matchedMemberThreshold) {
		if (matchedMemberThreshold == null
			|| matchedMemberThreshold.compareTo(BigDecimal.ZERO) < 0
			|| matchedMemberThreshold.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException("matched member threshold must be between 0 and 1");
		}
		this.matchedMemberThreshold = matchedMemberThreshold;
	}

	public BigDecimal calculateMemberScore(List<RecommendationTagScoreInput> inputs) {
		if (inputs == null || inputs.isEmpty()) {
			throw new IllegalArgumentException("recommendation tag score inputs must not be empty");
		}

		BigDecimal evidenceTotal = BigDecimal.ZERO;
		BigDecimal score = BigDecimal.ZERO;
		for (RecommendationTagScoreInput input : inputs) {
			validateRate(input.preferenceScore(), "preferenceScore");
			validateRate(input.normalizedPlaceTagEvidence(), "normalizedPlaceTagEvidence");
			evidenceTotal = evidenceTotal.add(input.normalizedPlaceTagEvidence());
			score = score.add(input.preferenceScore().multiply(input.normalizedPlaceTagEvidence()));
		}
		if (evidenceTotal.compareTo(BigDecimal.ONE) != 0) {
			throw new IllegalArgumentException("normalized place tag evidence must sum to 1");
		}
		return score.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
	}

	public BigDecimal calculateGroupScore(List<BigDecimal> memberScores) {
		if (memberScores == null || memberScores.isEmpty()) {
			return BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
		}
		BigDecimal score = BigDecimal.ZERO;
		for (BigDecimal memberScore : memberScores) {
			validateRate(memberScore, "memberScore");
			score = score.add(memberScore);
		}
		return score.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
	}

	public BigDecimal calculateGroupAverageScore(List<BigDecimal> memberScores) {
		if (memberScores == null || memberScores.isEmpty()) {
			return BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
		}
		return calculateGroupScore(memberScores)
			.divide(BigDecimal.valueOf(memberScores.size()), SCORE_SCALE, RoundingMode.HALF_UP);
	}

	public BigDecimal calculateGroupConsensusScore(List<BigDecimal> memberScores) {
		if (memberScores == null || memberScores.isEmpty()) {
			return BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
		}
		double logSum = 0;
		for (BigDecimal memberScore : memberScores) {
			validateRate(memberScore, "memberScore");
			if (memberScore.compareTo(BigDecimal.ZERO) == 0) {
				return BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
			}
			logSum += Math.log(memberScore.doubleValue());
		}
		return BigDecimal.valueOf(Math.exp(logSum / memberScores.size()))
			.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
	}

	public boolean isMatchedMember(BigDecimal memberScore) {
		validateRate(memberScore, "memberScore");
		return memberScore.compareTo(matchedMemberThreshold) >= 0;
	}

	private void validateRate(BigDecimal value, String name) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException(name + " must be between 0 and 1");
		}
	}
}
