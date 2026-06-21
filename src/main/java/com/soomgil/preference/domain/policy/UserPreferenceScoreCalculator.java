package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 태그 전체 좋아요율과 호불호 점수, 사용자 긍정/부정 근거로 개인 선호도를 계산한다.
 *
 * <p>호불호 점수는 {@code (1 - discrimination) / discrimination} 사전 강도로 변환한다.
 * 호불호가 클수록 개인 근거를 빠르게 반영하며 결과는 {@code 0..1} 범위다.
 */
public class UserPreferenceScoreCalculator {

	private static final int CALCULATION_SCALE = 12;
	private static final int STORAGE_SCALE = 6;

	public BigDecimal calculate(
		BigDecimal smoothedPositiveRate,
		BigDecimal preferenceDiscrimination,
		BigDecimal positiveEvidence,
		BigDecimal negativeEvidence
	) {
		validateRate(smoothedPositiveRate, "smoothedPositiveRate");
		validateRate(preferenceDiscrimination, "preferenceDiscrimination");
		validateEvidence(positiveEvidence, "positiveEvidence");
		validateEvidence(negativeEvidence, "negativeEvidence");

		BigDecimal userEvidence = positiveEvidence.add(negativeEvidence);
		if (userEvidence.compareTo(BigDecimal.ZERO) == 0
			|| preferenceDiscrimination.compareTo(BigDecimal.ZERO) == 0) {
			return smoothedPositiveRate.setScale(STORAGE_SCALE, RoundingMode.HALF_UP);
		}
		if (preferenceDiscrimination.compareTo(BigDecimal.ONE) == 0) {
			return positiveEvidence
				.divide(userEvidence, STORAGE_SCALE, RoundingMode.HALF_UP);
		}

		BigDecimal priorStrength = BigDecimal.ONE
			.subtract(preferenceDiscrimination)
			.divide(preferenceDiscrimination, CALCULATION_SCALE, RoundingMode.HALF_UP);
		BigDecimal numerator = priorStrength
			.multiply(smoothedPositiveRate)
			.add(positiveEvidence);
		BigDecimal denominator = priorStrength.add(userEvidence);
		return numerator.divide(denominator, STORAGE_SCALE, RoundingMode.HALF_UP);
	}

	private void validateRate(BigDecimal value, String name) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException(name + " must be between 0 and 1");
		}
	}

	private void validateEvidence(BigDecimal value, String name) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException(name + " must be greater than or equal to 0");
		}
	}
}
