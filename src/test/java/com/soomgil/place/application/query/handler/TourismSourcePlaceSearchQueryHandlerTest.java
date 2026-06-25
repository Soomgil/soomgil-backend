package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.port.PlaceIntroRaw;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedRequest;
import com.soomgil.place.application.port.TourismPlaceFeedResult;
import com.soomgil.place.application.port.TourismPlaceLiveSearchRequest;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TourismSourcePlaceSearchQueryHandlerTest {

	private RecordingTourismPlaceFeedClient liveClient;
	private TourismSourcePlaceSearchQueryHandler handler;

	@BeforeEach
	void setUp() {
		liveClient = new RecordingTourismPlaceFeedClient();
		handler = new TourismSourcePlaceSearchQueryHandler(liveClient);
	}

	@Test
	void searchesKtoLivePlacesWithQueryCriteria() {
		liveClient.items = List.of(new TourismPlaceFeedItem(
			"126508",
			"Haeundae Beach",
			"Busan Haeundae-gu",
			35.1587,
			129.1604,
			"https://cdn.soomgil.example.com/places/126508.jpg",
			"ATTRACTION",
			null,
			List.of("https://cdn.soomgil.example.com/places/126508.jpg")
		));

		PagedPlaceSummary result = handler.handle(new PlaceSearchQuery(
			"Busan beach",
			"129.0,35.0,130.0,36.0",
			"26000",
			"ATTRACTION",
			0,
			20
		));

		assertThat(liveClient.lastRequest.q()).isEqualTo("Busan beach");
		assertThat(liveClient.lastRequest.bbox()).isEqualTo("129.0,35.0,130.0,36.0");
		assertThat(liveClient.lastRequest.legalRegionCode()).isEqualTo("26000");
		assertThat(liveClient.lastRequest.category()).isEqualTo("ATTRACTION");
		assertThat(liveClient.lastRequest.limit()).isEqualTo(20);

		assertThat(result.items()).hasSize(1);
		PlaceSummary item = result.items().getFirst();
		assertThat(item.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(item.externalPlaceId()).isEqualTo("126508");
		assertThat(item.name()).isEqualTo("Haeundae Beach");
		assertThat(item.address()).isEqualTo("Busan Haeundae-gu");
		assertThat(item.lat()).isEqualTo(35.1587);
		assertThat(item.lng()).isEqualTo(129.1604);
		assertThat(item.thumbnailUrl()).isEqualTo(URI.create("https://cdn.soomgil.example.com/places/126508.jpg"));
		assertThat(item.category()).isEqualTo("ATTRACTION");
		assertThat(item.sourceStatus()).isEqualTo(PlaceSourceStatus.AVAILABLE);
		assertThat(result.page().totalElements()).isEqualTo(1L);
	}

	@Test
	void returnsEmptyPageWhenKtoLiveFindsNoPlaces() {
		PagedPlaceSummary result = handler.handle(new PlaceSearchQuery("unknown", null, null, null, 1, 20));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().page()).isEqualTo(1);
		assertThat(result.page().totalElements()).isZero();
	}

	@Test
	void returnsLiveKtoPlacesForJejuSearch() {
		liveClient.items = List.of(new TourismPlaceFeedItem(
			"126435",
			"성산일출봉",
			"제주특별자치도 서귀포시 성산읍 일출로 284-12",
			33.4581111,
			126.9415156,
			"https://tong.visitkorea.or.kr/cms/resource/00/2613500_image2_1.jpg",
			"관광지",
			null,
			List.of("https://tong.visitkorea.or.kr/cms/resource/00/2613500_image2_1.jpg")
		));

		PagedPlaceSummary result = handler.handle(new PlaceSearchQuery(
			"성산",
			"126.1,33.0,127.1,33.7",
			null,
			null,
			0,
			20
		));

		assertThat(liveClient.lastRequest.q()).isEqualTo("성산");
		assertThat(liveClient.lastRequest.bbox()).isEqualTo("126.1,33.0,127.1,33.7");
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().externalPlaceId()).isEqualTo("126435");
		assertThat(result.items().getFirst().name()).isEqualTo("성산일출봉");
		assertThat(result.page().totalElements()).isEqualTo(1L);
	}

	private static final class RecordingTourismPlaceFeedClient implements TourismPlaceFeedClient {

		private TourismPlaceLiveSearchRequest lastRequest;
		private List<TourismPlaceFeedItem> items = List.of();

		@Override
		public TourismPlaceFeedResult fetch(TourismPlaceFeedRequest request) {
			return new TourismPlaceFeedResult(List.of(), null);
		}

		@Override
		public List<TourismPlaceFeedItem> fetchLive(TourismPlaceLiveSearchRequest request) {
			lastRequest = request;
			return items;
		}

		@Override
		public PlaceIntroRaw fetchIntro(String contentId, String contentTypeId) {
			return PlaceIntroRaw.empty();
		}
	}
}
