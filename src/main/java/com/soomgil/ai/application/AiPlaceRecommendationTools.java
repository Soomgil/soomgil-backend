package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import org.springframework.ai.tool.annotation.Tool;

public final class AiPlaceRecommendationTools extends AiToolSupport {
	private final ListPlaceRecommendationsQueryHandler recommendationHandler;
	private final AiRecommendationViewportResolver viewportResolver;

	AiPlaceRecommendationTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		AiRecommendationViewportResolver viewportResolver
	) {
		super(request, auditService);
		this.recommendationHandler = recommendationHandler;
		this.viewportResolver = viewportResolver;
	}

	@Tool(description = "현재 여행방 멤버들의 취향을 종합해 장소를 추천한다. "
		+ "bbox는 비워두면 서버가 현재 지도 범위 또는 여행방 지역으로 자동 결정하므로 사용자에게 지역을 되묻지 말고 바로 호출한다. "
		+ "tab은 BASIC(기본) 또는 SUPER_LIKE(꼭 가고 싶은 곳)이며 모르면 비워둔다.")
	public Object recommendPlaces(RecommendPlacesInput input) {
		// 범위/탭 해석까지 execute 안에서 수행해, 실패해도 감사 기록과 WARN 로그가 남는다.
		return execute("recommendPlaces", AiToolExecutionPolicy.READ, input, null, () -> {
			String bbox = viewportResolver.resolveBbox(request, input.bbox());
			RecommendationTab tab = AiRecommendationViewportResolver.parseTab(input.tab());
			int size = Math.max(1, Math.min(input.size() == null ? 10 : input.size(), 10));
			boolean usableCenter = AiRecommendationViewportResolver.hasUsableCenter(input.centerLat(), input.centerLng());
			Double centerLat = usableCenter ? input.centerLat() : viewportResolver.centerLat(bbox);
			Double centerLng = usableCenter ? input.centerLng() : viewportResolver.centerLng(bbox);
			return recommendationHandler.handle(
				new ListPlaceRecommendationsQuery(tripId, bbox, centerLat, centerLng, tab, 0, size)
			);
		});
	}

	public record RecommendPlacesInput(String bbox, Double centerLat, Double centerLng, String tab, Integer size) {
	}
}
