package com.soomgil.place.application.port;

import java.util.List;

public record TourismPlaceFeedItem(
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String category,
	String description,
	List<String> photos
) {
	public TourismPlaceFeedItem {
		photos = photos == null ? List.of() : List.copyOf(photos);
	}
}
