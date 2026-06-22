package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import org.springframework.ai.tool.annotation.Tool;

public final class AiItineraryReadTools extends AiToolSupport {
	private final FindItineraryHandler itineraryHandler;

	AiItineraryReadTools(AiGuideRequest request, AiToolAuditService auditService, FindItineraryHandler itineraryHandler) {
		super(request, auditService);
		this.itineraryHandler = itineraryHandler;
	}

	@Tool(description = "현재 여행방의 일차, 일정 장소, 경로를 조회한다")
	public Object getCurrentItinerary() {
		return execute("getCurrentItinerary", AiToolExecutionPolicy.READ, null, null,
			() -> itineraryHandler.handle(new FindItineraryQuery(tripId, userId)));
	}
}
