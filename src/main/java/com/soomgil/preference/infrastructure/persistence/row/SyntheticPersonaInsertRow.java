package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 고정 합성 페르소나 upsert row.
 */
public record SyntheticPersonaInsertRow(
	String id,
	String personaKey,
	String displayName,
	String description,
	String generatorVersion,
	long seed,
	BigDecimal noiseRate
) {
}
