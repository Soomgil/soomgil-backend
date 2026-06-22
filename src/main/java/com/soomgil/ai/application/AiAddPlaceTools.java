package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

public final class AiAddPlaceTools extends AiToolSupport {
	private final AiItineraryToolService itineraryToolService;

	AiAddPlaceTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "사용자가 명시적으로 추가를 요청한 장소 하나를 지정한 일정 일차에 추가한다")
	public Object addPlaceToItinerary(AddPlaceInput input) {
		long version = baseVersion(input.baseVersion());
		return execute("addPlaceToItinerary", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, version,
			() -> itineraryToolService.addPlace(
				tripId, userId, version, new AiItineraryToolService.AddPlaceInput(
					input.itineraryDayId(), input.sortOrder(), input.placeProvider(), input.externalPlaceId(),
					input.placeName(), input.address(), input.lat(), input.lng(), uri(input.thumbnailUrl())
				)
			));
	}

	public record AddPlaceInput(
		Long baseVersion, UUID itineraryDayId, int sortOrder, String placeProvider, String externalPlaceId,
		String placeName, String address, Double lat, Double lng, String thumbnailUrl
	) {
	}
}
