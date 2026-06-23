package com.soomgil.place.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class KtoTourismPlaceClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void convertsKtoListAndDetailResponsesIntoFrontendPlaceData() throws Exception {
		var listBody = objectMapper.readTree("""
			{
			  "response": {"header": {"resultCode": "0000"}, "body": {"items": {"item": [{
			    "contentid": "126508", "title": "해운대해수욕장", "addr1": "부산 해운대구",
			    "mapx": "129.1604", "mapy": "35.1587", "contenttypeid": "12",
			    "firstimage": "https://img.example/main.jpg", "firstimage2": "https://img.example/sub.jpg",
			    "modifiedtime": "20260619173654"
			  }]}}}
			}
			""");
		var detailBody = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contentid":"126508", "overview":"<b>넓은 백사장이 있는 해수욕장</b>"
			}]}}}}
			""");

		var places = KtoTourismPlaceClient.parseList(listBody);
		var enriched = KtoTourismPlaceClient.withDetail(places.getFirst(), detailBody);

		assertThat(enriched.externalPlaceId()).isEqualTo("126508");
		assertThat(enriched.name()).isEqualTo("해운대해수욕장");
		assertThat(enriched.description()).isEqualTo("넓은 백사장이 있는 해수욕장");
		assertThat(enriched.photos()).containsExactly("https://img.example/main.jpg");
		assertThat(enriched.category()).isEqualTo("관광지");
		assertThat(enriched.sourceModifiedAt()).hasToString("2026-06-19T17:36:54+09:00");
	}

	@Test
	void usesKtoThumbnailOnlyWhenTheOriginalImageIsMissing() throws Exception {
		var listBody = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contentid":"126508","title":"해운대해수욕장","contenttypeid":"12",
			  "firstimage2":"https://img.example/thumbnail.jpg"
			}]}}}}
			""");

		var places = KtoTourismPlaceClient.parseList(listBody);

		assertThat(places.getFirst().photos()).containsExactly("https://img.example/thumbnail.jpg");
	}

	@Test
	void rejectsFailedKtoResponseInsteadOfReturningAnEmptyFeed() throws Exception {
		var body = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"30","resultMsg":"SERVICE KEY IS NOT REGISTERED"}}}
			""");

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> KtoTourismPlaceClient.parseList(body))
			.isInstanceOf(KtoTourismPlaceException.class)
			.hasMessageContaining("SERVICE KEY");
	}

	@Test
	void enrichesDetailsConcurrentlyWhilePreservingFeedOrder() throws Exception {
		var places = List.of(place("1"), place("2"), place("3"));
		var allStarted = new CountDownLatch(places.size());
		var release = new CountDownLatch(1);
		try (var detailExecutor = Executors.newFixedThreadPool(places.size())) {
			var result = CompletableFuture.supplyAsync(() ->
				KtoTourismPlaceClient.enrichDetailsConcurrently(places, place -> {
					allStarted.countDown();
					try {
						release.await();
					}
					catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException(exception);
					}
					return new TourismPlaceFeedItem(
						place.externalPlaceId(), place.name(), place.address(), place.lat(), place.lng(),
						place.thumbnailUrl(), place.category(), "detail-" + place.externalPlaceId(),
						place.photos(), place.sourceModifiedAt()
					);
				}, detailExecutor)
			);

			assertThat(allStarted.await(1, TimeUnit.SECONDS)).isTrue();
			release.countDown();
			assertThat(result.get(1, TimeUnit.SECONDS))
				.extracting(TourismPlaceFeedItem::description)
				.containsExactly("detail-1", "detail-2", "detail-3");
		}
	}

	private TourismPlaceFeedItem place(String id) {
		return new TourismPlaceFeedItem(
			id, "place-" + id, null, null, null, null, "12", null, List.of(), null
		);
	}
}
