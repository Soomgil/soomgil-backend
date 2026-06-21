package com.soomgil.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.api.dto.TagPreparationStatus;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagStateRow;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SwipeTagPreparationServiceTest {

	private final PreferenceSwipeFeedMapper mapper = mock(PreferenceSwipeFeedMapper.class);
	private final SwipeTagEnrichmentQueue queue = mock(SwipeTagEnrichmentQueue.class);
	private final SwipeTagPreparationService service = new SwipeTagPreparationService(mapper, queue);

	@Test
	void reusesFreshTagsWithoutEnqueueingAiWork() {
		var modifiedAt = OffsetDateTime.parse("2026-06-22T09:00:00+09:00");
		var place = place("fresh", modifiedAt);
		when(mapper.findTagStates(List.of("fresh"))).thenReturn(List.of(
			new SwipeFeedTagStateRow("fresh", modifiedAt, service.sourceHash(place))
		));
		when(mapper.findTags(List.of("fresh"))).thenReturn(List.of(tag("fresh", "자연")));

		var result = service.prepare(List.of(place)).get("fresh");

		assertThat(result.status()).isEqualTo(TagPreparationStatus.READY);
		assertThat(result.tags()).containsExactly("자연");
		verify(queue, never()).enqueue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void servesExistingTagsWhileRefreshingAChangedPlace() {
		var place = place("stale", OffsetDateTime.parse("2026-06-22T10:00:00+09:00"));
		when(mapper.findTagStates(List.of("stale"))).thenReturn(List.of(
			new SwipeFeedTagStateRow("stale", OffsetDateTime.parse("2026-06-21T10:00:00+09:00"), "old-hash")
		));
		when(mapper.findTags(List.of("stale"))).thenReturn(List.of(tag("stale", "박물관")));

		var result = service.prepare(List.of(place)).get("stale");

		assertThat(result.status()).isEqualTo(TagPreparationStatus.REFRESHING);
		assertThat(result.tags()).containsExactly("박물관");
		verify(queue).enqueue(org.mockito.ArgumentMatchers.eq(place), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void enqueuesMissingTagsAndReturnsPending() {
		var place = place("missing", null);
		when(mapper.findTagStates(List.of("missing"))).thenReturn(List.of());
		when(mapper.findTags(List.of("missing"))).thenReturn(List.of());

		var result = service.prepare(List.of(place)).get("missing");

		assertThat(result.status()).isEqualTo(TagPreparationStatus.PENDING);
		assertThat(result.tags()).isEmpty();
		verify(queue).enqueue(org.mockito.ArgumentMatchers.eq(place), org.mockito.ArgumentMatchers.any());
	}

	private TourismPlaceFeedItem place(String id, OffsetDateTime modifiedAt) {
		return new TourismPlaceFeedItem(
			id, "장소 " + id, "서울", 37.0, 127.0, null, "관광지", "설명", List.of(), modifiedAt
		);
	}

	private com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagRow tag(String id, String name) {
		return new com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagRow(id, name, 1);
	}
}
