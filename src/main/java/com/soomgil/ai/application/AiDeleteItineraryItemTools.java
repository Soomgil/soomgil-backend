package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

/** 사용자가 이름으로 지정한 일정 장소 하나를 삭제하는 AI 도구. */
public final class AiDeleteItineraryItemTools extends AiToolSupport {

	private final AiItineraryToolService itineraryToolService;

	AiDeleteItineraryItemTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "사용자가 이름을 지정한 기존 일정 장소 하나를 삭제한다. "
		+ "사용자가 말한 placeName을 그대로 전달한다. itemId는 현재 여행 맥락에서 확실히 확인한 경우에만 전달한다.")
	public Object deleteItineraryItem(DeleteItemInput input) {
		long version = baseVersion(input.baseVersion());
		return execute("deleteItineraryItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, version,
			() -> itineraryToolService.deleteItem(
				tripId, userId, version, input.itemId(), input.placeName()
			));
	}

	public record DeleteItemInput(Long baseVersion, UUID itemId, String placeName) {
	}
}
