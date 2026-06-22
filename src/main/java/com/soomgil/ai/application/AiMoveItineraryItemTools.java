package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

public final class AiMoveItineraryItemTools extends AiToolSupport {
	private final AiItineraryToolService itineraryToolService;

	AiMoveItineraryItemTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "기존 일정 장소 하나를 다른 일차 또는 순서로 이동한다. "
		+ "사용자가 말한 placeName과 목표 targetDayNumber를 그대로 전달한다. itemId와 itineraryDayId는 알고 있을 때만 전달한다.")
	public Object moveItineraryItem(MoveItemInput input) {
		long version = baseVersion(input.baseVersion());
		return execute("moveItineraryItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, version,
			() -> itineraryToolService.moveItem(
				tripId, userId, version, input.itemId(), input.placeName(), input.itineraryDayId(),
				input.targetDayNumber(), input.sortOrder()
			));
	}

	public record MoveItemInput(
		Long baseVersion,
		UUID itemId,
		String placeName,
		UUID itineraryDayId,
		Integer targetDayNumber,
		Integer sortOrder
	) {
	}
}
