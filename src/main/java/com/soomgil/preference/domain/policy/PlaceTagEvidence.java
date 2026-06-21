package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 한 장소 안에서 합계 1로 정규화된 태그 근거.
 *
 * @param tagId 태그 ID
 * @param value 정규화된 태그 근거
 */
public record PlaceTagEvidence(
	String tagId,
	BigDecimal value
) {
}
