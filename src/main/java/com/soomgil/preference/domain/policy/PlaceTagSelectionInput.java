package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 서버 장소 태그 selector가 평가할 후보.
 */
public record PlaceTagSelectionInput(
	String tagId,
	String tagCode,
	boolean activeSelectable,
	BigDecimal confidence,
	BigDecimal weight,
	BigDecimal preferenceDiscrimination,
	String tagStatisticRunId
) {
}
