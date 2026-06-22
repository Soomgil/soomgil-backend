package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 여행 일정 전체를 조회해 자연어 요약의 재료로 쓴다.
 *
 * <p>LLM이 일차별 장소와 동선을 직접 확인하면서 핵심 일정, 이동 거리, 대표 장소를
 * 3~5줄로 요약하도록 유도한다. 상태를 변경하지 않는 읽기 전용 도구다.
 */
public final class AiSummarizeItineraryTools extends AiToolSupport {

	private final FindItineraryHandler itineraryHandler;

	AiSummarizeItineraryTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		FindItineraryHandler itineraryHandler
	) {
		super(request, auditService);
		this.itineraryHandler = itineraryHandler;
	}

	@Tool(description = "여행방의 전체 일차와 장소를 조회한다. 요약, 분석, 조언을 할 때 반드시 이 도구로 최신 일정을 확인한다.")
	public Object summarizeItinerary() {
		return execute("summarizeItinerary", AiToolExecutionPolicy.READ, null, null,
			() -> itineraryHandler.handle(new FindItineraryQuery(tripId, userId)));
	}
}
