package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 사용자 태그 가중치에 필요한 태그 중요도와 선호 점수를 계산한다.
 *
 * <p>태그 중요도는 {@code 0.7 + 0.3 * preferenceDiscrimination}이며 {@code 0.7..1.0}
 * 범위다. 선호 점수 계산은 사용자 근거와 serving 통계에 기반한 정책에 위임한다.
 */
public class UserPreferenceWeightCalculator {

	private static final BigDecimal BASE_IMPORTANCE = new BigDecimal("0.7");
	private static final BigDecimal DISCRIMINATION_FACTOR = new BigDecimal("0.3");
	private static final int STORAGE_SCALE = 6;

	private final UserPreferenceScoreCalculator preferenceScoreCalculator =
		new UserPreferenceScoreCalculator();

	/**
	 * 태그 취향 구분력을 사용자 근거 계산용 중요도로 변환한다.
	 *
	 * @param preferenceDiscrimination {@code 0..1} 범위의 취향 구분력
	 * @return {@code 0.7..1.0} 범위의 태그 중요도
	 */
	public BigDecimal calculateTagImportance(BigDecimal preferenceDiscrimination) {
		if (preferenceDiscrimination == null
			|| preferenceDiscrimination.compareTo(BigDecimal.ZERO) < 0
			|| preferenceDiscrimination.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException("preferenceDiscrimination must be between 0 and 1");
		}
		return BASE_IMPORTANCE
			.add(DISCRIMINATION_FACTOR.multiply(preferenceDiscrimination))
			.setScale(STORAGE_SCALE, RoundingMode.HALF_UP);
	}

	/**
	 * serving 통계와 사용자의 최종 positive/negative 근거로 선호 점수를 계산한다.
	 */
	public BigDecimal calculatePreferenceScore(
		BigDecimal smoothedPositiveRate,
		BigDecimal preferenceDiscrimination,
		BigDecimal positiveEvidence,
		BigDecimal negativeEvidence
	) {
		return preferenceScoreCalculator.calculate(
			smoothedPositiveRate,
			preferenceDiscrimination,
			positiveEvidence,
			negativeEvidence
		);
	}
}
