package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSavedPlaceMapper;
import com.soomgil.preference.infrastructure.persistence.row.SavedPlaceRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PreferenceSavedPlaceServiceTest {

	@Test
	void hydratesKtoSavedPlaceMissingFromLocalTourismTables() {
		UUID userId = UUID.randomUUID();
		PreferenceSavedPlaceMapper mapper = mock(PreferenceSavedPlaceMapper.class);
		TourismPlaceFeedClient tourismClient = mock(TourismPlaceFeedClient.class);
		@SuppressWarnings("unchecked")
		ObjectProvider<CurrentUserProvider> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(() -> new CurrentUser(userId, "min@example.com"));
		when(mapper.countSavedPlaces(userId.toString())).thenReturn(1L);
		when(mapper.listSavedPlaces(userId.toString(), 20, 0)).thenReturn(List.of(new SavedPlaceRow(
			UUID.randomUUID().toString(), "KTO", "3089161", null, null, null, null, null, null,
			OffsetDateTime.parse("2026-06-25T00:00:00+09:00")
		)));
		when(tourismClient.fetchOne("3089161")).thenReturn(Optional.of(new TourismPlaceFeedItem(
			"3089161", "호텔컬리넌 제주", "제주특별자치도 제주시", 33.499, 126.531,
			"https://img.example/jeju.jpg", "숙박", "제주 여행 숙소",
			List.of("https://img.example/jeju.jpg")
		)));

		var service = new PreferenceSavedPlaceService(provider, mapper, tourismClient);
		var result = service.list(new ListSavedPlacesQuery(0, 20));

		assertThat(result.items()).singleElement().satisfies(saved -> {
			assertThat(saved.place().name()).isEqualTo("호텔컬리넌 제주");
			assertThat(saved.place().address()).isEqualTo("제주특별자치도 제주시");
			assertThat(saved.place().thumbnailUrl()).hasToString("https://img.example/jeju.jpg");
		});
	}
}
