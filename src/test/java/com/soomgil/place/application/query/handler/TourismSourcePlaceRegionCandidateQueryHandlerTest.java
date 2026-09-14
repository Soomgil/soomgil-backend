package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.place.application.port.KtoRegionCode;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceLiveSearchRequest;
import com.soomgil.place.application.query.dto.PlaceRegionCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.service.LegalRegionKtoCodeResolver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TourismSourcePlaceRegionCandidateQueryHandlerTest {
	private final TourismPlaceFeedClient liveClient = mock(TourismPlaceFeedClient.class);
	private final LegalRegionKtoCodeResolver resolver = mock(LegalRegionKtoCodeResolver.class);
	private final TourismSourcePlaceRegionCandidateQueryHandler handler =
		new TourismSourcePlaceRegionCandidateQueryHandler(liveClient, resolver);

	@Test
	@DisplayName("법정동 코드를 관광공사 area/sigungu 코드로 바꿔 조회한다")
	void translatesLegalRegionCodesBeforeCallingTheFeed() {
		when(resolver.resolve(List.of("5011000000"))).thenReturn(List.of(new KtoRegionCode("39", "4")));
		when(liveClient.fetchLive(any())).thenReturn(List.of(item("126434", "제주목 관아")));

		List<PlaceViewportCandidate> result = handler.handle(
			new PlaceRegionCandidateQuery(List.of("5011000000"), "제주", null, 10)
		);

		ArgumentCaptor<TourismPlaceLiveSearchRequest> captor = ArgumentCaptor.forClass(TourismPlaceLiveSearchRequest.class);
		verify(liveClient).fetchLive(captor.capture());
		assertThat(captor.getValue().legalRegionCode()).isEqualTo("39");
		assertThat(captor.getValue().sigunguCode()).isEqualTo("4");
		assertThat(captor.getValue().q()).isNull();
		assertThat(result).extracting(PlaceViewportCandidate::externalPlaceId).containsExactly("126434");
	}

	@Test
	@DisplayName("변환되는 지역이 없으면 대표 목적지 검색어로 대체한다")
	void fallsBackToKeywordWhenNoRegionResolves() {
		when(resolver.resolve(List.of("9911000000"))).thenReturn(List.of());
		when(liveClient.fetchLive(any())).thenReturn(List.of(item("1", "어딘가")));

		handler.handle(new PlaceRegionCandidateQuery(List.of("9911000000"), "제주", null, 10));

		ArgumentCaptor<TourismPlaceLiveSearchRequest> captor = ArgumentCaptor.forClass(TourismPlaceLiveSearchRequest.class);
		verify(liveClient).fetchLive(captor.capture());
		assertThat(captor.getValue().q()).isEqualTo("제주");
		assertThat(captor.getValue().legalRegionCode()).isNull();
	}

	@Test
	@DisplayName("여러 지역에서 같은 장소가 나오면 한 번만 남긴다")
	void deduplicatesPlacesAcrossRegions() {
		when(resolver.resolve(List.of("5011000000", "5013000000")))
			.thenReturn(List.of(new KtoRegionCode("39", "4"), new KtoRegionCode("39", "3")));
		when(liveClient.fetchLive(any())).thenReturn(List.of(item("126434", "제주목 관아")));

		List<PlaceViewportCandidate> result = handler.handle(
			new PlaceRegionCandidateQuery(List.of("5011000000", "5013000000"), null, null, 10)
		);

		assertThat(result).hasSize(1);
	}

	private static TourismPlaceFeedItem item(String id, String name) {
		return new TourismPlaceFeedItem(id, name, "제주특별자치도 제주시", 33.5, 126.5, null, "관광지", null, List.of(), null);
	}
}
