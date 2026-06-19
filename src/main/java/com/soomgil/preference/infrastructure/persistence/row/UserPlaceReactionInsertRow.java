package com.soomgil.preference.infrastructure.persistence.row;

import java.time.OffsetDateTime;

/**
 * 사용자 장소 최종 반응 insert row.
 */
public record UserPlaceReactionInsertRow(
	String id,
	String userId,
	String provider,
	String externalPlaceId,
	String reaction,
	OffsetDateTime sourceModifiedAt
) {
}
