package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

public record SwipeReactionRefreshRow(
	String id,
	String userId,
	String reaction,
	String placeTagEnrichmentId,
	String source,
	String sourceResourceId,
	BigDecimal evidenceMultiplier
) {
}
