package com.soomgil.preference.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedResult;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.social.application.query.dto.FolloweePlaceReaction;
import com.soomgil.social.application.query.handler.FindFolloweePlaceReactionsQueryHandler;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PreferenceSwipeFeedFolloweeReactionTest {

	@Test
	void attachesFolloweeReactionsToEachFeedPlace() {
		UUID currentUserId = UUID.fromString("00000000-0000-0000-0000-000000001101");
		UUID followeeId = UUID.fromString("00000000-0000-0000-0000-000000001102");
		PlaceRef place = new PlaceRef(PlaceProvider.KTO, "126508");
		UserSummary followee = new UserSummary(
			followeeId,
			"여행 친구",
			URI.create("https://cdn.soomgil.example.com/users/followee.jpg")
		);

		@SuppressWarnings("unchecked")
		ObjectProvider<CurrentUserProvider> currentUserProvider = mock(ObjectProvider.class);
		PreferenceSwipeFeedMapper feedMapper = mock(PreferenceSwipeFeedMapper.class);
		TourismPlaceFeedClient placeClient = mock(TourismPlaceFeedClient.class);
		FindFolloweePlaceReactionsQueryHandler reactionHandler =
			mock(FindFolloweePlaceReactionsQueryHandler.class);

		when(currentUserProvider.getIfAvailable())
			.thenReturn(() -> new CurrentUser(currentUserId, "min@example.com"));
		when(placeClient.fetch(any())).thenReturn(new TourismPlaceFeedResult(
			List.of(new TourismPlaceFeedItem(
				"126508",
				"Haeundae Beach",
				"Busan Haeundae-gu",
				35.1587,
				129.1604,
				"https://cdn.soomgil.example.com/places/126508.jpg",
				"관광지",
				"Beach",
				List.of("https://cdn.soomgil.example.com/places/126508.jpg")
			)), "next"));
		when(feedMapper.findReactions(currentUserId.toString(), List.of("126508"))).thenReturn(List.of());
		when(feedMapper.findTags(List.of("126508"))).thenReturn(List.of());
		when(reactionHandler.handle(any()))
			.thenReturn(List.of(new FolloweePlaceReaction(place, followee)));

		var handler = new PreferenceSwipeFeedQueryHandler(
			currentUserProvider,
			placeClient,
			feedMapper,
			reactionHandler
		);

		var response = handler.handle(new SwipeFeedQuery(null, null, 20, true, null));

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().likedByFollowees()).containsExactly(followee);
	}
}
