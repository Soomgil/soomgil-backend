package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceFeedRequest;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.application.port.KtoRegionCode;
import com.soomgil.place.application.service.KtoContentTypeResolver;
import com.soomgil.place.application.service.LegalRegionKtoCodeResolver;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import com.soomgil.preference.api.dto.SwipeFeedItem;
import com.soomgil.preference.api.dto.SwipeFeedPlace;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.application.service.SwipeTagPreparation;
import com.soomgil.preference.application.service.SwipeTagPreparationService;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedReactionRow;
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
	private final SwipeTagPreparationService tagPreparationService;
	private final PlaceAccessibilityCacheService accessibilityCacheService;
	private final LegalRegionKtoCodeResolver regionCodeResolver;

	public PreferenceSwipeFeedQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		TourismPlaceFeedClient placeFeedClient,
		PreferenceSwipeFeedMapper mapper,
		FindFolloweePlaceReactionsQueryHandler followeeReactionQueryHandler,
		SwipeTagPreparationService tagPreparationService,
		PlaceAccessibilityCacheService accessibilityCacheService,
		LegalRegionKtoCodeResolver regionCodeResolver
	) {
		this.currentUserProvider = currentUserProvider;
		this.placeFeedClient = placeFeedClient;
		this.mapper = mapper;
		this.followeeReactionQueryHandler = followeeReactionQueryHandler;
		this.tagPreparationService = tagPreparationService;
		this.accessibilityCacheService = accessibilityCacheService;
		this.regionCodeResolver = regionCodeResolver;
	}

	@Transactional(readOnly = true)
	@Override
	public SwipeFeedResponse handle(SwipeFeedQuery query) {
		UUID userId = currentUserId();
		int limit = normalizeLimit(query.limit());
		var remoteFeed = placeFeedClient.fetch(new TourismPlaceFeedRequest(
			resolveKtoAreaCode(query.legalRegionCode()),
			query.category(),
			limit,
			query.seed(),
            query.excludeRecent() ? mapper.findReactedPlaceIds(userId.toString()) : List.of()
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
		Map<String, SwipeTagPreparation> tagPreparations = selectedIds.isEmpty()
			? Map.of() : tagPreparationService.prepare(selectedPlaces);
		Map<PlaceRef, List<UserSummary>> likedByFollowees = findLikedByFollowees(selectedPlaces);
		Map<String, PlaceAccessibilityInfo> accessibilityMap = Map.of();
		List<SwipeFeedItem> items = selectedPlaces.stream()
			.map(place -> toItem(place, reactions, tagPreparations, likedByFollowees, accessibilityMap))
			.toList();

		return new SwipeFeedResponse(items, remoteFeed.nextSeed());
	}

	/**
	 * 프론트가 준 법정동 코드를 관광 원천이 이해하는 KTO 시도 areaCode로 바꾼다. 이미 KTO 코드이거나
	 * 대응이 없으면 원래 값을 그대로 둔다(구 데모 호환). 이 변환이 없으면 지역 필터가 legalRegion 코드를
	 * area_code와 직접 비교해 전국 어디서도 결과가 비게 된다.
	 */
	private String resolveKtoAreaCode(String legalRegionCode) {
		if (legalRegionCode == null || legalRegionCode.isBlank()) {
			return legalRegionCode;
		}
		List<KtoRegionCode> resolved = regionCodeResolver.resolve(List.of(legalRegionCode.strip()));
		return resolved.isEmpty() ? legalRegionCode : resolved.get(0).areaCode();
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

	private Map<String, PlaceAccessibilityInfo> fetchAccessibility(List<TourismPlaceFeedItem> places) {
		if (places.isEmpty()) {
			return Map.of();
		}
		List<PlaceAccessibilityCacheService.PlaceRef> refs = places.stream()
			.map(place -> new PlaceAccessibilityCacheService.PlaceRef(
				PlaceProvider.KTO.name(),
				place.externalPlaceId(),
				KtoContentTypeResolver.contentTypeIdFor(place.category())
			))
			.toList();
		return accessibilityCacheService.getMany(refs);
	}

	private SwipeFeedItem toItem(
		TourismPlaceFeedItem place,
		Map<String, SwipeReaction> reactions,
		Map<String, SwipeTagPreparation> tagPreparations,
		Map<PlaceRef, List<UserSummary>> likedByFollowees,
		Map<String, PlaceAccessibilityInfo> accessibilityMap
	) {
		PlaceRef placeRef = toPlaceRef(place);
		SwipeTagPreparation tagPreparation = tagPreparations.getOrDefault(
			place.externalPlaceId(),
			new SwipeTagPreparation(List.of(), com.soomgil.preference.api.dto.TagPreparationStatus.PENDING)
		);
		PlaceAccessibilityInfo accessibility = accessibilityMap.getOrDefault(
			PlaceProvider.KTO.name() + ":" + place.externalPlaceId(),
			PlaceAccessibilityInfo.unknown()
		);
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
				tagPreparation.tags(),
				tagPreparation.status(),
				accessibility
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
