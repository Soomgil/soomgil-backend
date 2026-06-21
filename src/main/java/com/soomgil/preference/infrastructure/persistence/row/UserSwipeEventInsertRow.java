package com.soomgil.preference.infrastructure.persistence.row;

import java.time.OffsetDateTime;

/**
 * 사용자 스와이프 이벤트 로그 insert row.
 */
public record UserSwipeEventInsertRow(
	String userId,
	String provider,
	String externalPlaceId,
	String reaction,
	String previousReaction,
	String placeTagEnrichmentId,
	OffsetDateTime sourceModifiedAt
) {
}
