package com.soomgil.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class DefaultSwipeTagEnrichmentQueueTest {

	@Test
	void deduplicatesTheSamePlaceVersionWhileItIsQueued() {
		ArrayDeque<Runnable> tasks = new ArrayDeque<>();
		Executor executor = tasks::add;
		SwipeTagEnrichmentProcessor processor = mock(SwipeTagEnrichmentProcessor.class);
		DefaultSwipeTagEnrichmentQueue queue = new DefaultSwipeTagEnrichmentQueue(executor, processor);
		TourismPlaceFeedItem place = new TourismPlaceFeedItem(
			"126508", "해운대", "부산", 35.0, 129.0, null, "관광지", "설명", List.of()
		);

		queue.enqueue(place, "same-hash");
		queue.enqueue(place, "same-hash");

		assertThat(tasks).hasSize(1);
		tasks.remove().run();
		verify(processor).process(place, "same-hash");
		verifyNoMoreInteractions(processor);
	}
}
