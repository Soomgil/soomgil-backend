package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 저장 장소 insert row.
 */
public record SavedPlaceInsertRow(
	String id,
	String userId,
	String provider,
	String externalPlaceId
) {
}
