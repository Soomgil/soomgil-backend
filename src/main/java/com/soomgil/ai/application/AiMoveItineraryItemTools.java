package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

public final class AiMoveItineraryItemTools extends AiToolSupport {
	private final UpdateItineraryItemHandler updateItemHandler;

	AiMoveItineraryItemTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		UpdateItineraryItemHandler updateItemHandler
	) {
		super(request, auditService);
		this.updateItemHandler = updateItemHandler;
	}

	@Tool(description = "기존 일정 항목을 다른 일차 또는 순서로 이동한다")
	public Object moveItineraryItem(MoveItemInput input) {
		long version = baseVersion(input.baseVersion());
		return execute("moveItineraryItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, version,
			() -> updateItemHandler.handle(new UpdateItineraryItemCommand(
				tripId, userId, version, input.itemId(), input.itineraryDayId(), input.sortOrder(),
				null, null, null, null, null
			)));
	}

	public record MoveItemInput(Long baseVersion, UUID itemId, UUID itineraryDayId, Integer sortOrder) {
	}
}
