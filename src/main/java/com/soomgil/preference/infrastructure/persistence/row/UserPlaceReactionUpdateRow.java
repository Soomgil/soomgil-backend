package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 사용자 장소 최종 반응 update row.
 */
public record UserPlaceReactionUpdateRow(
	String id,
	String reaction,
	String placeTagEnrichmentId,
	OffsetDateTime sourceModifiedAt,
	String source,
	String sourceResourceId,
	BigDecimal evidenceMultiplier
) {
}
