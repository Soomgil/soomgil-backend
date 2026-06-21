package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 합성 페르소나의 장소 반응을 결정하는 입력.
 *
 * @param personaKey 페르소나 catalog key
 * @param provider 장소 provider
 * @param externalPlaceId provider의 외부 장소 ID
 * @param seed 페르소나에 고정된 재현 seed
 * @param personaPlaceScore 태그 성향과 장소 태그 근거를 합산한 점수
 * @param hardLikeMatched 장소가 hard like 태그에 해당하는지
 * @param hardDislikeMatched 장소가 hard dislike 태그에 해당하는지
 */
public record SyntheticPersonaSwipeInput(
	String personaKey,
	String provider,
	String externalPlaceId,
	long seed,
	BigDecimal personaPlaceScore,
	boolean hardLikeMatched,
	boolean hardDislikeMatched
) {
}
