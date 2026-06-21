package com.soomgil.preference.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DefaultSwipeTagEnrichmentQueue implements SwipeTagEnrichmentQueue {

	private static final Logger log = LoggerFactory.getLogger(DefaultSwipeTagEnrichmentQueue.class);
	private final Executor executor;
	private final SwipeTagEnrichmentProcessor processor;
	private final Set<String> queuedKeys = ConcurrentHashMap.newKeySet();

	public DefaultSwipeTagEnrichmentQueue(
		@Qualifier("swipeTagExecutor") Executor executor,
		SwipeTagEnrichmentProcessor processor
	) {
		this.executor = executor;
		this.processor = processor;
	}

	@Override
	public void enqueue(TourismPlaceFeedItem place, String sourceHash) {
		String key = place.externalPlaceId();
		if (!queuedKeys.add(key)) {
			return;
		}
		try {
			executor.execute(() -> {
				try {
					processor.process(place, sourceHash);
				}
				catch (RuntimeException exception) {
					log.warn("Swipe tag enrichment failed for KTO place {}", place.externalPlaceId(), exception);
				}
				finally {
					queuedKeys.remove(key);
				}
			});
		}
		catch (RuntimeException exception) {
			queuedKeys.remove(key);
			throw exception;
		}
	}

	@Override
	public boolean isQueued(String externalPlaceId) {
		return queuedKeys.contains(externalPlaceId);
	}
}
