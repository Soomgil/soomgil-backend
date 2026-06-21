package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedRequest;
import com.soomgil.preference.api.dto.SwipeFeedItem;
import com.soomgil.preference.api.dto.SwipeFeedPlace;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagRow;
import com.soomgil.social.application.query.dto.FindFolloweePlaceReactionsQuery;
import com.soomgil.social.application.query.dto.FolloweePlaceReaction;
import com.soomgil.social.application.query.handler.FindFolloweePlaceReactionsQueryHandler;
import com.soomgil.user.api.dto.UserSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관광 원천 장소를 사용해 개인 스와이프 feed를 조회한다.
 */
@Service
public class PreferenceSwipeFeedQueryHandler implements SwipeFeedQueryHandler {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final TourismPlaceFeedClient placeFeedClient;
	private final PreferenceSwipeFeedMapper mapper;
	private final FindFolloweePlaceReactionsQueryHandler followeeReactionQueryHandler;

	public PreferenceSwipeFeedQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		TourismPlaceFeedClient placeFeedClient,
		PreferenceSwipeFeedMapper mapper,
		FindFolloweePlaceReactionsQueryHandler followeeReactionQueryHandler
	) {
		this.currentUserProvider = currentUserProvider;
		this.placeFeedClient = placeFeedClient;
		this.mapper = mapper;
		this.followeeReactionQueryHandler = followeeReactionQueryHandler;
	}

	@Transactional(readOnly = true)
	@Override
	public SwipeFeedResponse handle(SwipeFeedQuery query) {
		UUID userId = currentUserId();
		int limit = normalizeLimit(query.limit());
		var remoteFeed = placeFeedClient.fetch(new TourismPlaceFeedRequest(
			query.legalRegionCode(),
			query.category(),
			limit,
			query.seed()
		));
		List<String> candidateIds = remoteFeed.items().stream()
			.map(TourismPlaceFeedItem::externalPlaceId)
			.toList();
		Map<String, SwipeReaction> reactions = candidateIds.isEmpty()
			? Map.of()
			: mapper.findReactions(userId.toString(), candidateIds).stream()
				.collect(Collectors.toMap(SwipeFeedReactionRow::externalPlaceId, row -> toReaction(row.reaction())));
		List<TourismPlaceFeedItem> selectedPlaces = remoteFeed.items().stream()
			.filter(place -> !query.excludeRecent() || !reactions.containsKey(place.externalPlaceId()))
			.limit(limit)
			.toList();
		List<String> selectedIds = selectedPlaces.stream().map(TourismPlaceFeedItem::externalPlaceId).toList();
		Map<String, List<String>> tags = selectedIds.isEmpty() ? Map.of() : mapper.findTags(selectedIds).stream()
			.collect(Collectors.groupingBy(
				SwipeFeedTagRow::externalPlaceId,
				Collectors.mapping(SwipeFeedTagRow::displayName, Collectors.toList())
			));
		Map<PlaceRef, List<UserSummary>> likedByFollowees = findLikedByFollowees(selectedPlaces);
		List<SwipeFeedItem> items = selectedPlaces.stream()
			.map(place -> toItem(place, reactions, tags, likedByFollowees))
			.toList();

		return new SwipeFeedResponse(items, remoteFeed.nextSeed());
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to read swipe feed.");
		}
		return provider.currentUserId();
	}

	private Map<PlaceRef, List<UserSummary>> findLikedByFollowees(List<TourismPlaceFeedItem> rows) {
		List<PlaceRef> places = rows.stream()
			.map(this::toPlaceRef)
			.toList();

		return followeeReactionQueryHandler.handle(new FindFolloweePlaceReactionsQuery(places))
			.stream()
			.collect(Collectors.groupingBy(
				FolloweePlaceReaction::place,
				Collectors.mapping(FolloweePlaceReaction::followee, Collectors.toList())
			));
	}

	private SwipeFeedItem toItem(
		TourismPlaceFeedItem place,
		Map<String, SwipeReaction> reactions,
		Map<String, List<String>> tags,
		Map<PlaceRef, List<UserSummary>> likedByFollowees
	) {
		PlaceRef placeRef = toPlaceRef(place);
		return new SwipeFeedItem(
			new SwipeFeedPlace(
				PlaceProvider.KTO,
				place.externalPlaceId(),
				place.name(),
				place.address(),
				place.lat(),
				place.lng(),
				place.thumbnailUrl(),
				place.category(),
				PlaceSourceStatus.AVAILABLE,
				place.description(),
				place.photos(),
				tags.getOrDefault(place.externalPlaceId(), List.of())
			),
			reactions.get(place.externalPlaceId()),
			likedByFollowees.getOrDefault(placeRef, List.of())
		);
	}

	private PlaceRef toPlaceRef(TourismPlaceFeedItem place) {
		return new PlaceRef(PlaceProvider.KTO, place.externalPlaceId());
	}

	private SwipeReaction toReaction(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return SwipeReaction.valueOf(value);
	}

	private int normalizeLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
