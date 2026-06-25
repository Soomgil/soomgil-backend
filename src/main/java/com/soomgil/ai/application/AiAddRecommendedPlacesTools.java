package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 추천 장소를 조회한 뒤 여러 장소를 일정에 추가하는 복합 쓰기 도구.
 *
 * <p>장소 후보를 직접 만들어내지 않고 기존 추천 query를 통과한 후보만 추가한다. 이미
 * 일정에 들어간 provider/externalPlaceId 조합은 중복 추가하지 않는다.
 */
public final class AiAddRecommendedPlacesTools extends AiToolSupport {

	private static final int DEFAULT_LIMIT = 3;
	private static final int MAX_LIMIT = 10;

	private final ListPlaceRecommendationsQueryHandler recommendationHandler;
	private final AiItineraryToolService itineraryToolService;

	AiAddRecommendedPlacesTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.recommendationHandler = recommendationHandler;
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "멤버 취향 기반 추천 장소를 조회하고, 상위 후보 여러 개를 일정에 한 번에 추가한다. "
		+ "사용자가 '추천 여행지 알아서 넣어줘', '갈 만한 곳 몇 개 일정에 추가해줘'처럼 추천과 추가를 함께 요청할 때 사용한다. "
		+ "bbox는 현재 지도 viewport를 사용하고, 일차가 불명확하면 itineraryDayId를 null로 두어 일차 미정에 추가한다. "
		+ "limit는 최대 10개다. 이미 일정에 있는 장소는 도구 내부에서 건너뛴다.")
	public Object addRecommendedPlacesToItinerary(AddRecommendedPlacesInput input) {
		long version = baseVersion(input.baseVersion());
		return execute(
			"addRecommendedPlacesToItinerary",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input,
			version,
			() -> addRecommendedPlaces(version, input)
		);
	}

	private BulkAddRecommendedPlacesResult addRecommendedPlaces(long baseVersion, AddRecommendedPlacesInput input) {
		String bbox = input.bbox();
		if (bbox == null || bbox.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "추천 장소를 추가하려면 지도 범위가 필요해요.");
		}
		int limit = Math.max(1, Math.min(input.limit() == null ? DEFAULT_LIMIT : input.limit(), MAX_LIMIT));
		RecommendationTab tab = parseTab(input.tab());
		var page = recommendationHandler.handle(new ListPlaceRecommendationsQuery(
			tripId, bbox, input.centerLat(), input.centerLng(), tab, 0, Math.max(limit * 2, limit)
		));
		Set<String> existingRefs = existingPlaceRefs();
		List<AddedPlace> added = new ArrayList<>();
		List<String> skipped = new ArrayList<>();
		long version = baseVersion;
		int sortOrder = input.startSortOrder() == null ? 0 : input.startSortOrder();
		ItineraryMutationResult last = null;
		List<com.soomgil.preference.api.dto.PlaceRecommendation> recommendations =
			page.items() == null ? List.of() : page.items();
		for (var recommendation : recommendations) {
			PlaceSummary place = recommendation.place();
			String ref = ref(place);
			if (existingRefs.contains(ref)) {
				skipped.add(place.name());
				continue;
			}
			last = itineraryToolService.addPlace(
				tripId,
				userId,
				version,
				new AiItineraryToolService.AddPlaceInput(
					input.itineraryDayId(), sortOrder++, place.provider().name(), place.externalPlaceId(),
					place.name(), place.address(), place.lat(), place.lng(), place.thumbnailUrl()
				)
			);
			version = last.itineraryVersion();
			existingRefs.add(ref);
			added.add(new AddedPlace(place.name(), place.provider().name(), place.externalPlaceId(), recommendation.recommendationReason()));
			if (added.size() >= limit) break;
		}
		return new BulkAddRecommendedPlacesResult(baseVersion, last == null ? baseVersion : last.itineraryVersion(), added, skipped);
	}

	private Set<String> existingPlaceRefs() {
		Set<String> refs = new HashSet<>();
		if (request.tripContext() == null) return refs;
		for (AiTripContext.DaySummary day : request.tripContext().days()) {
			for (AiTripContext.ItemSummary item : day.items()) {
				if (item.placeProvider() != null && item.externalPlaceId() != null) {
					refs.add(item.placeProvider() + ":" + item.externalPlaceId());
				}
			}
		}
		return refs;
	}

	private String ref(PlaceSummary place) {
		return place.provider().name() + ":" + place.externalPlaceId();
	}

	private RecommendationTab parseTab(String tab) {
		if (tab == null || tab.isBlank()) return RecommendationTab.BASIC;
		return RecommendationTab.valueOf(tab.trim().toUpperCase());
	}

	public record AddRecommendedPlacesInput(
		Long baseVersion,
		String bbox,
		Double centerLat,
		Double centerLng,
		String tab,
		Integer limit,
		UUID itineraryDayId,
		Integer startSortOrder
	) {
	}

	public record BulkAddRecommendedPlacesResult(
		long versionBefore,
		long versionAfter,
		List<AddedPlace> addedPlaces,
		List<String> skippedAlreadyScheduled
	) {
	}

	public record AddedPlace(String placeName, String provider, String externalPlaceId, String reason) {
	}
}
