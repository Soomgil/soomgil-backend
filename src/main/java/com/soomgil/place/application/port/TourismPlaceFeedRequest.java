package com.soomgil.place.application.port;

public record TourismPlaceFeedRequest(
	String legalRegionCode,
	String category,
	int limit,
	String seed
) {
}
