package com.soomgil.place.application.port;

import java.util.List;

public record TourismPlaceFeedResult(
	List<TourismPlaceFeedItem> items,
	String nextSeed
) {
	public TourismPlaceFeedResult {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
