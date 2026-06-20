package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 태그별 보정 좋아요율과 호불호 균형 점수를 계산한다.
 *
 * <p>호불호 점수는 긍정과 부정이 균형일수록 커지며, 반응 수가 사전 반응 수보다 적으면
 * 실제 반응 비율만큼 낮아진다.
 */
public class PreferenceDiscriminationCalculator {

	private static final BigDecimal FOUR = BigDecimal.valueOf(4);
	private static final int CALCULATION_SCALE = 12;
	private static final int STORAGE_SCALE = 6;

	public TagPreferenceStatistics calculate(
		long positiveCount,
		long reactionCount,
		BigDecimal globalPositiveRate,
		BigDecimal priorReactionCount
	) {
		validate(positiveCount, reactionCount, globalPositiveRate, priorReactionCount);

		BigDecimal reactions = BigDecimal.valueOf(reactionCount);
		BigDecimal denominator = reactions.add(priorReactionCount);
		BigDecimal smoothedPositiveRate = BigDecimal.valueOf(positiveCount)
			.add(priorReactionCount.multiply(globalPositiveRate))
			.divide(denominator, CALCULATION_SCALE, RoundingMode.HALF_UP);
		BigDecimal reactionReliability = reactions
			.divide(denominator, CALCULATION_SCALE, RoundingMode.HALF_UP);
		BigDecimal polarization = FOUR
			.multiply(smoothedPositiveRate)
			.multiply(BigDecimal.ONE.subtract(smoothedPositiveRate))
			.multiply(reactionReliability);

		return new TagPreferenceStatistics(
			smoothedPositiveRate.setScale(STORAGE_SCALE, RoundingMode.HALF_UP),
			polarization.setScale(STORAGE_SCALE, RoundingMode.HALF_UP)
		);
	}

	private void validate(
		long positiveCount,
		long reactionCount,
		BigDecimal globalPositiveRate,
		BigDecimal priorReactionCount
	) {
		if (reactionCount < 1) {
			throw new IllegalArgumentException("reactionCount must be greater than 0");
		}
		if (positiveCount < 0 || positiveCount > reactionCount) {
			throw new IllegalArgumentException("positiveCount must be between 0 and reactionCount");
		}
		if (globalPositiveRate == null
			|| globalPositiveRate.compareTo(BigDecimal.ZERO) < 0
			|| globalPositiveRate.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException("globalPositiveRate must be between 0 and 1");
		}
		if (priorReactionCount == null || priorReactionCount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("priorReactionCount must be greater than 0");
		}
	}
}
