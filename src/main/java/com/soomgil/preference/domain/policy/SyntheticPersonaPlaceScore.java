package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 합성 페르소나의 장소 점수와 hard 성향 일치 여부.
 *
 * @param score 태그 성향, confidence, weight를 합산한 signed score
 * @param hardLikeMatched 하나 이상의 hard like 태그가 유효하게 일치했는지
 * @param hardDislikeMatched 하나 이상의 hard dislike 태그가 유효하게 일치했는지
 */
public record SyntheticPersonaPlaceScore(
	BigDecimal score,
	boolean hardLikeMatched,
	boolean hardDislikeMatched
) {
}
