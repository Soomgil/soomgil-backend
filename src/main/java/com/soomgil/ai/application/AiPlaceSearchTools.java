package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import org.springframework.ai.tool.annotation.Tool;

public final class AiPlaceSearchTools extends AiToolSupport {
	private final PlaceSearchQueryHandler placeSearchHandler;

	AiPlaceSearchTools(AiGuideRequest request, AiToolAuditService auditService, PlaceSearchQueryHandler placeSearchHandler) {
		super(request, auditService);
		this.placeSearchHandler = placeSearchHandler;
	}

	@Tool(description = "한국 관광지 후보를 검색한다. 일정에 추가하지 않고 후보만 반환한다")
	public Object searchPlaces(SearchPlacesInput input) {
		return execute("searchPlaces", AiToolExecutionPolicy.READ, input, null,
			() -> placeSearchHandler.handle(new PlaceSearchQuery(
				input.query(), input.bbox(), input.legalRegionCode(), input.category(), 0, 10
			)));
	}

	public record SearchPlacesInput(String query, String bbox, String legalRegionCode, String category) {
	}
}
