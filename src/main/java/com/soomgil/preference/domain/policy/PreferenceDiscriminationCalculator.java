package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 태그별 보정 긍정률과 취향 구분력을 계산한다.
 *
 * <p>취향 구분력은 태그의 보정 긍정률이 전체 긍정률과 얼마나 다른지를 전체 긍정률의
 * 가능한 최대 거리로 정규화한 값이다. 좋아함/싫어함의 방향은 이 값이 아니라 사용자별
 * preference score가 표현한다.
 */
public class PreferenceDiscriminationCalculator {

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
		BigDecimal distance = smoothedPositiveRate.subtract(globalPositiveRate).abs();
		BigDecimal maximumDistance = globalPositiveRate.max(BigDecimal.ONE.subtract(globalPositiveRate));
		BigDecimal preferenceDiscrimination = distance
			.divide(maximumDistance, CALCULATION_SCALE, RoundingMode.HALF_UP)
			.max(BigDecimal.ZERO)
			.min(BigDecimal.ONE);

		return new TagPreferenceStatistics(
			smoothedPositiveRate.setScale(STORAGE_SCALE, RoundingMode.HALF_UP),
			preferenceDiscrimination.setScale(STORAGE_SCALE, RoundingMode.HALF_UP)
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
