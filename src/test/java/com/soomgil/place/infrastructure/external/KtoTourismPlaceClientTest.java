package com.soomgil.place.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.place.application.port.PlaceIntroRaw;
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
	void extractsAccessibilityFieldsFromKtoDetailIntro() throws Exception {
		var body = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contenttypeid":"12","usetime":"09:00~18:00","restdate":"매주 월요일",
			  "parking":"무료 주차 가능",
			  "chkbabycarriage":"가능","chkpet":"불가능"
			}]}}}}
			""");

		PlaceIntroRaw intro = KtoTourismPlaceClient.parseIntro(body, "12");

		assertThat(intro.useTime()).isEqualTo("09:00~18:00");
		assertThat(intro.restDate()).isEqualTo("매주 월요일");
		assertThat(intro.parking()).isEqualTo("무료 주차 가능");
		assertThat(intro.disability()).isNull();
		assertThat(intro.chkBabyCarriage()).isEqualTo("가능");
		assertThat(intro.chkPet()).isEqualTo("불가능");
	}

	@Test
	void extractsContentTypeSpecificKtoIntroFields() throws Exception {
		var cultureBody = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contenttypeid":"14","usetimeculture":"10:00~19:00","restdateculture":"월요일",
			  "parkingculture":"유료","chkbabycarriageculture":"가능","chkpetculture":"불가능"
			}]}}}}
			""");
		var leisureBody = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contenttypeid":"28","usetimeleports":"09:00~17:00","restdateleports":"화요일",
			  "parkingleports":"무료","chkbabycarriageleports":"불가능","chkpetleports":"가능"
			}]}}}}
			""");
		var shoppingBody = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contenttypeid":"38","opentime":"11:00~20:00","restdateshopping":"수요일",
			  "parkingshopping":"주차 가능","chkbabycarriageshopping":"가능","chkpetshopping":"가능"
			}]}}}}
			""");

		assertThat(KtoTourismPlaceClient.parseIntro(cultureBody, "14").useTime()).isEqualTo("10:00~19:00");
		assertThat(KtoTourismPlaceClient.parseIntro(leisureBody, "28").parking()).isEqualTo("무료");
		assertThat(KtoTourismPlaceClient.parseIntro(shoppingBody, "38").restDate()).isEqualTo("수요일");
	}

	@Test
	void mergesBarrierFreeTourFieldsWithOperatingInformation() throws Exception {
		var body = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contentid":"2633956","publictransport":"출입구까지 휠체어 접근 가능",
			  "restroom":"장애인 화장실 있음","stroller":"유모차 대여 가능"
			}]}}}}
			""");
		var intro = new PlaceIntroRaw("09:00~18:00", "월요일", "무료", null, null, "불가능");

		PlaceIntroRaw merged = KtoTourismPlaceClient.mergeIntro(
			intro,
			KtoTourismPlaceClient.parseBarrierFree(body)
		);

		assertThat(merged.useTime()).isEqualTo("09:00~18:00");
		assertThat(merged.disability()).contains("휠체어 접근 가능", "장애인 화장실 있음");
		assertThat(merged.chkBabyCarriage()).isEqualTo("유모차 대여 가능");
		assertThat(merged.chkPet()).isEqualTo("불가능");
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
