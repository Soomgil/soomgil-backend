package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

/**
 * LLM이 판단한 조건(장애인 이용 불가, 유모차 진입 불가, 유료 시설 등)에 해당하는
 * 일정 항목을 삭제하는 쓰기 도구.
 *
 * <p>DB에 accessibility/is_paid 메타데이터가 없으므로, LLM이 placeName/address를
 * 근거로 삭제 대상을 판단한다. 삭제할 항목 ID 목록만 전달하도록 강제한다.
 */
public final class AiFilterPlacesTools extends AiToolSupport {

	private final AiItineraryToolService itineraryToolService;

	AiFilterPlacesTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "사용자가 명시한 조건(예: 유료 시설, 장애인 이용 불가, 유모차 진입 불가)에 해당하는 일정 항목들을 삭제한다. "
		+ "여행 맥락 JSON의 days[].items[] 안내를 근거로 삭제할 itemId 목록을 직접 구성하라. "
		+ "삭제 후에는 항목 이름과 삭제 이유를 자연어로 설명하라.")
	public Object removeItineraryItemsByCondition(RemoveItemsInput input) {
		long version = baseVersion(input.baseVersion());
		return execute(
			"removeItineraryItemsByCondition",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, version,
			() -> itineraryToolService.removeItems(tripId, userId, version, input.itemIds())
		);
	}

	public record RemoveItemsInput(Long baseVersion, List<UUID> itemIds) {
	}
}
