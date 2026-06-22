package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import org.springframework.ai.tool.annotation.Tool;

public final class AiPlaceRecommendationTools extends AiToolSupport {
	private final ListPlaceRecommendationsQueryHandler recommendationHandler;

	AiPlaceRecommendationTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		ListPlaceRecommendationsQueryHandler recommendationHandler
	) {
		super(request, auditService);
		this.recommendationHandler = recommendationHandler;
	}

	@Tool(description = "현재 여행방 멤버들의 취향을 종합해 지도 범위 안의 장소를 추천한다")
	public Object recommendPlaces(RecommendPlacesInput input) {
		RecommendationTab tab = input.tab() == null || input.tab().isBlank()
			? RecommendationTab.BASIC : RecommendationTab.valueOf(input.tab().trim().toUpperCase());
		int size = Math.max(1, Math.min(input.size() == null ? 10 : input.size(), 10));
		ListPlaceRecommendationsQuery query = new ListPlaceRecommendationsQuery(
			tripId, bbox(input.bbox()), centerLat(input.centerLat()), centerLng(input.centerLng()), tab, 0, size
		);
		return execute("recommendPlaces", AiToolExecutionPolicy.READ, input, null,
			() -> recommendationHandler.handle(query));
	}

	private String bbox(String supplied) {
		if (supplied != null && !supplied.isBlank()) return supplied;
		if (request.viewport() == null || request.viewport().minLng() == null
			|| request.viewport().minLat() == null || request.viewport().maxLng() == null
			|| request.viewport().maxLat() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Recommendation viewport is required.");
		}
		return request.viewport().minLng() + "," + request.viewport().minLat() + ","
			+ request.viewport().maxLng() + "," + request.viewport().maxLat();
	}

	private Double centerLat(Double supplied) {
		return supplied != null ? supplied
			: request.viewport() == null || request.viewport().minLat() == null || request.viewport().maxLat() == null
				? null : (request.viewport().minLat() + request.viewport().maxLat()) / 2;
	}

	private Double centerLng(Double supplied) {
		return supplied != null ? supplied
			: request.viewport() == null || request.viewport().minLng() == null || request.viewport().maxLng() == null
				? null : (request.viewport().minLng() + request.viewport().maxLng()) / 2;
	}

	public record RecommendPlacesInput(String bbox, Double centerLat, Double centerLng, String tab, Integer size) {
	}
}
