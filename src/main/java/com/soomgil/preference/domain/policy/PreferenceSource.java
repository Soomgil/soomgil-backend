package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 사용자 취향 근거가 생성된 제품 경험을 구분한다.
 *
 * <p>가입 설문은 콜드 스타트 추천을 안정시키기 위해 일반 반응보다 세 배의 근거를 만든다.
 * 원본 source와 적용 배수는 반응 및 이벤트 로그에 함께 보관해 반응 변경 시 이전 근거를 정확히 되돌린다.
 */
public enum PreferenceSource {
	ONBOARDING(new BigDecimal("3.0")),
	HOME_BACKGROUND(BigDecimal.ONE),
	TRIP_VOTE(BigDecimal.ONE);

	private final BigDecimal evidenceMultiplier;

	PreferenceSource(BigDecimal evidenceMultiplier) {
		this.evidenceMultiplier = evidenceMultiplier;
	}

	/**
	 * 이 source의 장소 반응 하나에 적용할 근거 배수를 반환한다.
	 *
	 * @return 1 이상 3 이하의 근거 배수
	 */
	public BigDecimal evidenceMultiplier() {
		return evidenceMultiplier;
	}
}
