package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 합성 페르소나 장소 점수에 사용할 확정 태그 입력.
 *
 * @param tagCode 고정 whitelist 태그 code
 * @param confidence 장소에 태그가 맞는 정도
 * @param weight 장소에서 태그가 차지하는 중요도
 */
public record SyntheticPlaceTagInput(
	String tagCode,
	BigDecimal confidence,
	BigDecimal weight
) {
}
