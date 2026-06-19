package com.soomgil.preference.infrastructure.persistence.row;

import java.time.OffsetDateTime;

/**
 * 저장 장소 조회 row.
 */
public record SavedPlaceRow(
	String id,
	String provider,
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String category,
	OffsetDateTime createdAt
) {
}
