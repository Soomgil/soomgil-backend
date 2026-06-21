package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 장소 확정 태그의 근거 비율 계산 입력.
 *
 * @param tagId 태그 ID
 * @param confidence 장소에 태그가 맞는 정도
 * @param weight 장소에서 태그가 차지하는 중요도
 */
public record PlaceTagEvidenceInput(
	String tagId,
	BigDecimal confidence,
	BigDecimal weight
) {
}
