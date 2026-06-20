package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 페르소나별 whitelist 태그 성향 저장 row.
 */
public record SyntheticPersonaTagPreferenceInsertRow(
	String personaId,
	String tagCode,
	String preferenceType,
	BigDecimal preferenceStrength
) {
}
