package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.place.application.port.AwardPhotoCatalogClient;
import com.soomgil.place.application.port.AwardPhotoCatalogItem;
import com.soomgil.place.application.query.dto.ListAwardPhotosQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class KtoListAwardPhotosQueryHandlerTest {

	private static final ZoneId KOREA_TIME = ZoneId.of("Asia/Seoul");

	@Test
	void splitsFilmLocationIntoRegionAndPlaceName() {
		var handler = handlerOn(dayOfYear(1), catalogOf(
			item("A", "경상남도 합천군, 가야산국립공원", "48")
		));

		var photos = handler.handle(new ListAwardPhotosQuery(10, null));

		assertThat(photos).hasSize(1);
		assertThat(photos.getFirst().regionName()).isEqualTo("경상남도 합천군");
		assertThat(photos.getFirst().placeName()).isEqualTo("가야산국립공원");
		assertThat(photos.getFirst().filmYearMonth()).isEqualTo("2024-01");
	}

	@Test
	void treatsFilmLocationWithoutSeparatorAsPlaceNameOnly() {
		var handler = handlerOn(dayOfYear(1), catalogOf(
			item("A", "가야산국립공원", "48")
		));

		var photos = handler.handle(new ListAwardPhotosQuery(10, null));

		assertThat(photos.getFirst().placeName()).isEqualTo("가야산국립공원");
		assertThat(photos.getFirst().regionName()).isNull();
	}

	@Test
	void filtersByRegionCode() {
		var handler = handlerOn(dayOfYear(1), catalogOf(
			item("A", "경상남도 합천군, 가야산국립공원", "48"),
			item("B", "서울특별시 종로구, 경복궁", "11")
		));

		var photos = handler.handle(new ListAwardPhotosQuery(10, "11"));

		assertThat(photos).hasSize(1);
		assertThat(photos.getFirst().awardContentId()).isEqualTo("B");
	}

	@Test
	void rotatesStartingPositionByFiveItemsEachDaySoTheSameDayIsStable() {
		var catalog = catalogOf(
			item("A", "지역 A, 장소 A", "11"),
			item("B", "지역 B, 장소 B", "11"),
			item("C", "지역 C, 장소 C", "11"),
			item("D", "지역 D, 장소 D", "11"),
			item("E", "지역 E, 장소 E", "11"),
			item("F", "지역 F, 장소 F", "11"),
			item("G", "지역 G, 장소 G", "11")
		);

		var firstDay = handlerOn(dayOfYear(1), catalog).handle(new ListAwardPhotosQuery(2, null));
		var firstDayAgain = handlerOn(dayOfYear(1), catalog).handle(new ListAwardPhotosQuery(2, null));
		var secondDay = handlerOn(dayOfYear(2), catalog).handle(new ListAwardPhotosQuery(2, null));

		assertThat(firstDay).isEqualTo(firstDayAgain);
		assertThat(firstDay.getFirst().awardContentId()).isEqualTo("A");
		assertThat(secondDay.getFirst().awardContentId()).isEqualTo("F");
	}

	@Test
	void limitNeverExceedsCatalogSize() {
		var handler = handlerOn(dayOfYear(1), catalogOf(
			item("A", "지역 A, 장소 A", "11")
		));

		assertThat(handler.handle(new ListAwardPhotosQuery(50, null))).hasSize(1);
	}

	@Test
	void returnsEmptyListWhenCatalogIsUnavailable() {
		var handler = handlerOn(dayOfYear(1), List::of);

		assertThat(handler.handle(new ListAwardPhotosQuery(10, null))).isEmpty();
	}

	private static KtoListAwardPhotosQueryHandler handlerOn(Clock clock, AwardPhotoCatalogClient client) {
		return new KtoListAwardPhotosQueryHandler(client, clock);
	}

	private static AwardPhotoCatalogClient catalogOf(AwardPhotoCatalogItem... items) {
		List<AwardPhotoCatalogItem> catalog = List.of(items);
		return () -> catalog;
	}

	private static AwardPhotoCatalogItem item(String id, String filmLocation, String regionCode) {
		return new AwardPhotoCatalogItem(
			id,
			id + " 작품",
			filmLocation,
			"촬영자",
			"디지털카메라 부문 [입선]",
			"202401",
			"https://img.example/" + id + ".jpg",
			"https://img.example/" + id + "-thumb.jpg",
			"Type1",
			regionCode,
			"키워드"
		);
	}

	private static Clock dayOfYear(int dayOfYear) {
		return Clock.fixed(
			Instant.parse("2026-01-01T00:00:00Z").plusSeconds((dayOfYear - 1) * 86_400L),
			KOREA_TIME
		);
	}
}
