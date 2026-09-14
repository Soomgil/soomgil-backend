package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.itinerary.domain.model.RouteMode;
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

	@Tool(description = "특정 일차의 장소들을 현재 일정 순서대로 모두 경로 연결한다. "
		+ "도보는 WALKING, 자전거길은 CYCLING, 자동차길은 DRIVING을 mode로 지정한다. "
		+ "이미 연결된 구간은 중복 생성하지 않고 mode가 다르면 해당 이동수단으로 다시 계산한다.")
	public Object connectDayRoutes(ConnectDayRoutesInput input) {
		long version = baseVersion(input.baseVersion());
		return execute(
			"connectDayRoutes",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, version,
			() -> itineraryToolService.connectDayRoutes(
				tripId, userId, version, input.itineraryDayId(), input.dayNumber(), input.mode()
			)
		);
	}

	public record OptimizeRouteInput(Long baseVersion, List<AiItineraryToolService.ItemMove> moves) {
	}

	public record ConnectDayRoutesInput(
		Long baseVersion,
		UUID itineraryDayId,
		Integer dayNumber,
		RouteMode mode
	) {
	}
}
