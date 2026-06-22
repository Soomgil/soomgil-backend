package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 여행 동선 최적화 도구.
 *
 * <p>LLM이 여행 맥락 JSON의 days[].items[].lat,lng 를 보고 가까운 장소끼리 같은
 * 일차로 묶도록 reorder plan을 구성한다. 각 이동은 baseVersion을 앞선 결과로
 * 갱신해 순차 적용한다.
 */
public final class AiOptimizeRouteTools extends AiToolSupport {

	private final AiItineraryToolService itineraryToolService;

	AiOptimizeRouteTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "여행 동선을 최적화하기 위해 일정 항목들을 다른 일차로 옮기고 정렬한다. "
		+ "여행 맥락 JSON의 days[].items[].lat,lng 와 days[].id 를 근거로 가까운 장소끼리 묶어 "
		+ "이동 순서를 재구성하라. 각 move마다 대상 일차 ID와 sort_order를 지정한다.")
	public Object optimizeRoute(OptimizeRouteInput input) {
		long version = baseVersion(input.baseVersion());
		return execute(
			"optimizeRoute",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, version,
			() -> itineraryToolService.reorderItems(tripId, userId, version, input.moves())
		);
	}

	public record OptimizeRouteInput(Long baseVersion, List<AiItineraryToolService.ItemMove> moves) {
	}
}
