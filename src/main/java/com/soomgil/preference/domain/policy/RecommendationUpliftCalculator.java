package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 같은 dataset과 K에서 측정한 baseline 대비 개선 metric의 상승률을 계산한다.
 */
public class RecommendationUpliftCalculator {

	private static final int PERCENT_SCALE = 6;

	public BigDecimal calculatePercent(BigDecimal baselineMetric, BigDecimal improvedMetric) {
		if (baselineMetric == null || improvedMetric == null
			|| baselineMetric.compareTo(BigDecimal.ZERO) < 0
			|| improvedMetric.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("recommendation metrics must not be negative");
		}
		if (baselineMetric.compareTo(BigDecimal.ZERO) == 0) {
			if (improvedMetric.compareTo(BigDecimal.ZERO) == 0) {
				return BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
			}
			throw new IllegalArgumentException("uplift percent is undefined when baseline is zero");
		}
		return improvedMetric.subtract(baselineMetric)
			.divide(baselineMetric, PERCENT_SCALE + 2, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(100))
			.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
	}
}
