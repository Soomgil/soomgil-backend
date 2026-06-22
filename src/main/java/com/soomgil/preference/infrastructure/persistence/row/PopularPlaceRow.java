package com.soomgil.preference.infrastructure.persistence.row;

public record PopularPlaceRow(
	String provider,
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String category,
	long savedCount
) {
}
