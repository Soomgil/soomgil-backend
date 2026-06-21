package com.soomgil.preference.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedItem;

public interface SwipeTagEnrichmentQueue {
	void enqueue(TourismPlaceFeedItem place, String sourceHash);

	boolean isQueued(String externalPlaceId);
}
