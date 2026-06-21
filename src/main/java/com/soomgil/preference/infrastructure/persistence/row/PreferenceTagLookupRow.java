package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 태그 code로 preference tag를 찾은 결과 row.
 */
public record PreferenceTagLookupRow(
	String id,
	String code,
	Boolean activeSelectable,
	BigDecimal preferenceDiscrimination,
	String tagStatisticRunId
) {
}
