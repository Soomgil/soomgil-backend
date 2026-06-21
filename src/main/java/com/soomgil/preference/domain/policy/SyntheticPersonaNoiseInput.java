package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * soft 성향 점수에 결정적 잡음을 적용하기 위한 입력.
 */
public record SyntheticPersonaNoiseInput(
	String personaKey,
	String provider,
	String externalPlaceId,
	long seed,
	BigDecimal noiseRate,
	BigDecimal score,
	boolean hardPreferenceMatched
) {
}
