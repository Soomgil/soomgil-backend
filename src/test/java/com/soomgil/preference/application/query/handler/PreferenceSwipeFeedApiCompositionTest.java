package com.soomgil.preference.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedResult;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagRow;
import com.soomgil.social.application.query.handler.FindFolloweePlaceReactionsQueryHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PreferenceSwipeFeedApiCompositionTest {

	@Test
	void combinesKtoPlaceDataWithStoredTagsAndReaction() {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000001201");
		@SuppressWarnings("unchecked")
		ObjectProvider<CurrentUserProvider> currentUserProvider = mock(ObjectProvider.class);
		TourismPlaceFeedClient placeClient = mock(TourismPlaceFeedClient.class);
		PreferenceSwipeFeedMapper mapper = mock(PreferenceSwipeFeedMapper.class);
		FindFolloweePlaceReactionsQueryHandler followees = mock(FindFolloweePlaceReactionsQueryHandler.class);

		when(currentUserProvider.getIfAvailable())
			.thenReturn(() -> new CurrentUser(userId, "min@example.com"));
		when(placeClient.fetch(org.mockito.ArgumentMatchers.any())).thenReturn(new TourismPlaceFeedResult(
			List.of(new TourismPlaceFeedItem(
				"126508", "해운대해수욕장", "부산 해운대구", 35.1587, 129.1604,
				"https://img.example/main.jpg", "12", "넓은 백사장이 있는 해수욕장",
				List.of("https://img.example/main.jpg", "https://img.example/sub.jpg")
			)),
			"next-seed"
		));
		when(mapper.findReactions(userId.toString(), List.of("126508")))
			.thenReturn(List.of(new SwipeFeedReactionRow("126508", "LIKE")));
		when(mapper.findTags(List.of("126508")))
			.thenReturn(List.of(
				new SwipeFeedTagRow("126508", "바다·해안", 1),
				new SwipeFeedTagRow("126508", "산책", 2)
			));
		when(followees.handle(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

		var handler = new PreferenceSwipeFeedQueryHandler(
			currentUserProvider, placeClient, mapper, followees
		);
		var response = handler.handle(new SwipeFeedQuery(null, null, 20, false, "seed"));

		assertThat(response.nextSeed()).isEqualTo("next-seed");
		assertThat(response.items()).singleElement().satisfies(item -> {
			assertThat(item.place().externalPlaceId()).isEqualTo("126508");
			assertThat(item.place().description()).isEqualTo("넓은 백사장이 있는 해수욕장");
			assertThat(item.place().photos()).hasSize(2);
			assertThat(item.place().tags()).containsExactly("바다·해안", "산책");
			assertThat(item.myReaction().name()).isEqualTo("LIKE");
		});
	}
}
