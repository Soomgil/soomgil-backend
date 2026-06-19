package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.preference.api.dto.SwipeFeedItem;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedPlaceRow;
import com.soomgil.social.application.query.dto.FindFolloweePlaceReactionsQuery;
import com.soomgil.social.application.query.dto.FolloweePlaceReaction;
import com.soomgil.social.application.query.handler.FindFolloweePlaceReactionsQueryHandler;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
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
	private final PreferenceSwipeFeedMapper mapper;
	private final FindFolloweePlaceReactionsQueryHandler followeeReactionQueryHandler;

	public PreferenceSwipeFeedQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSwipeFeedMapper mapper,
		FindFolloweePlaceReactionsQueryHandler followeeReactionQueryHandler
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
		this.followeeReactionQueryHandler = followeeReactionQueryHandler;
	}

	@Transactional(readOnly = true)
	@Override
	public SwipeFeedResponse handle(SwipeFeedQuery query) {
		UUID userId = currentUserId();
		List<SwipeFeedPlaceRow> rows = mapper.findFeed(
				userId.toString(),
				query.legalRegionCode(),
				query.category(),
				normalizeLimit(query.limit()),
				query.excludeRecent()
			);
		Map<PlaceRef, List<UserSummary>> likedByFollowees = findLikedByFollowees(rows);
		List<SwipeFeedItem> items = rows.stream()
			.map(row -> toItem(row, likedByFollowees))
			.toList();

		return new SwipeFeedResponse(items, null);
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to read swipe feed.");
		}
		return provider.currentUserId();
	}

	private Map<PlaceRef, List<UserSummary>> findLikedByFollowees(List<SwipeFeedPlaceRow> rows) {
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
		SwipeFeedPlaceRow row,
		Map<PlaceRef, List<UserSummary>> likedByFollowees
	) {
		PlaceRef placeRef = toPlaceRef(row);
		return new SwipeFeedItem(
			new PlaceSummary(
				PlaceProvider.KTO,
				String.valueOf(row.contentId()),
				row.title(),
				row.address(),
				row.latitude(),
				row.longitude(),
				toUri(row.thumbnailUrl()),
				row.category(),
				PlaceSourceStatus.AVAILABLE
			),
			toReaction(row.myReaction()),
			likedByFollowees.getOrDefault(placeRef, List.of())
		);
	}

	private PlaceRef toPlaceRef(SwipeFeedPlaceRow row) {
		return new PlaceRef(PlaceProvider.KTO, String.valueOf(row.contentId()));
	}

	private SwipeReaction toReaction(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return SwipeReaction.valueOf(value);
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}

	private int normalizeLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
