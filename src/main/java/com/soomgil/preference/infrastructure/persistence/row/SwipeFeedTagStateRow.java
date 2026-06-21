package com.soomgil.preference.infrastructure.persistence.row;

import java.time.OffsetDateTime;

public record SwipeFeedTagStateRow(
	String externalPlaceId,
	OffsetDateTime sourceModifiedAt,
	String sourceHash
) {
}
