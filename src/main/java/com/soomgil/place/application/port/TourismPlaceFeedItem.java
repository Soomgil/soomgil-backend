package com.soomgil.place.application.port;

import java.util.List;
import java.time.OffsetDateTime;

public record TourismPlaceFeedItem(
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String category,
	String description,
	List<String> photos,
	OffsetDateTime sourceModifiedAt
) {
	public TourismPlaceFeedItem {
		photos = photos == null ? List.of() : List.copyOf(photos);
	}

	public TourismPlaceFeedItem(
		String externalPlaceId, String name, String address, Double lat, Double lng,
		String thumbnailUrl, String category, String description, List<String> photos
	) {
		this(externalPlaceId, name, address, lat, lng, thumbnailUrl, category, description, photos, null);
	}
}
