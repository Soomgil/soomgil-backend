package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일정 분석 결과로 체크리스트 항목을 자동 생성한다.
 *
 * <p>LLM이 일정을 분석해 예약 필수 여부, 날씨 대비, 이동 수단 등 판단한 뒤, 추가할
 * 항목 리스트를 전달한다. 대상 checklist가 없으면 scope/dayId로 먼저 생성하고,
 * 이후 항목들을 한 번에 추가한다.
 */
public final class AiGenerateChecklistTools extends AiToolSupport {

	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateChecklistItemCommandHandler checklistItemHandler;

	AiGenerateChecklistTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		UpsertChecklistCommandHandler checklistHandler,
		CreateChecklistItemCommandHandler checklistItemHandler
	) {
		super(request, auditService);
		this.checklistHandler = checklistHandler;
		this.checklistItemHandler = checklistItemHandler;
	}

	@Tool(description = "현재 일정을 분석해 자동으로 체크리스트 항목을 추가한다. "
		+ "예: 에버랜드/오월드 같은 예약 필수 장소가 있으면 '오월드 예약', 테마파크면 '편한 신발 챙기기'. "
		+ "여행방 전체 공통 준비물에만 사용한다. 특정 일차의 장소에서 나온 할 일은 generateChecklistItemsByDay를 사용한다. "
		+ "대상 checklist가 있으면 checklistId를 쓰고, 없으면 checklistId를 null로 두면 scope/dayId로 새로 만든다.")
	@Transactional
	public Object generateChecklistItems(GenerateItemsInput input) {
		return execute(
			"generateChecklistItems",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, null,
			() -> buildChecklistItems(input)
		);
	}

	@Tool(description = "현재 여행 일정을 분석해 일차별 체크리스트에 항목을 추가한다. "
		+ "예: 롯데월드가 3일차에 있으면 3일차 itineraryDayId 그룹에 '롯데월드 예매 확인'을 넣는다. "
		+ "장소가 속한 days[].id를 itineraryDayId로 반드시 전달하고, 여행방 전체 체크리스트에 몰아넣지 않는다. "
		+ "일차가 다른 항목은 dayGroups를 나누어 전달한다.")
	@Transactional
	public Object generateChecklistItemsByDay(GenerateItemsByDayInput input) {
		return execute(
			"generateChecklistItemsByDay",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, null,
			() -> buildChecklistItemsByDay(input)
		);
	}

	private BulkChecklistGenerationResult buildChecklistItemsByDay(GenerateItemsByDayInput input) {
		if (input.dayGroups() == null || input.dayGroups().isEmpty()) {
			return new BulkChecklistGenerationResult(0, 0, List.of());
		}
		int checklistCount = 0;
		int itemCount = 0;
		List<DayChecklistResult> results = new java.util.ArrayList<>();
		for (DayChecklistInput dayGroup : input.dayGroups()) {
			if (dayGroup == null || dayGroup.itineraryDayId() == null
				|| dayGroup.items() == null || dayGroup.items().isEmpty()) {
				continue;
			}
			PlanningMutationResponse response = buildChecklistItems(new GenerateItemsInput(
				dayGroup.checklistId(),
				"DAY",
				dayGroup.itineraryDayId(),
				dayGroup.title() == null ? "일차 체크리스트" : dayGroup.title(),
				dayGroup.items(),
				dayGroup.startSortOrder()
			));
			if (response != null && response.checklist() != null) {
				checklistCount++;
				long created = dayGroup.items().stream()
					.filter(content -> content != null && !content.isBlank())
					.count();
				itemCount += (int) created;
				results.add(new DayChecklistResult(dayGroup.itineraryDayId(), response.checklist().id(), (int) created));
			}
		}
		return new BulkChecklistGenerationResult(checklistCount, itemCount, results);
	}

	private PlanningMutationResponse buildChecklistItems(GenerateItemsInput input) {
		UUID checklistId = input.checklistId();
		PlanningMutationResponse lastResponse = null;
		if (checklistId == null) {
			PlanningScopeType scope = PlanningScopeType.valueOf(input.scope().trim().toUpperCase());
			lastResponse = checklistHandler.handle(new UpsertChecklistCommand(
				tripId, userId, scope, input.itineraryDayId(),
				input.title() == null ? "자동 생성 체크리스트" : input.title()
			));
			if (lastResponse != null && lastResponse.checklist() != null) {
				checklistId = lastResponse.checklist().id();
			}
		}
		if (checklistId == null) {
			return lastResponse;
		}
		int sortOrder = input.startSortOrder() == null ? 0 : input.startSortOrder();
		for (String content : input.items()) {
			if (content == null || content.isBlank()) continue;
			lastResponse = checklistItemHandler.handle(new CreateChecklistItemCommand(
				tripId, checklistId, userId, content, sortOrder++
			));
		}
		return lastResponse;
	}

	public record GenerateItemsInput(
		UUID checklistId,
		String scope,
		UUID itineraryDayId,
		String title,
		List<String> items,
		Integer startSortOrder
	) {
	}

	public record GenerateItemsByDayInput(List<DayChecklistInput> dayGroups) {
	}

	public record DayChecklistInput(
		UUID checklistId,
		UUID itineraryDayId,
		String title,
		List<String> items,
		Integer startSortOrder
	) {
	}

	public record BulkChecklistGenerationResult(
		int checklistCount,
		int itemCount,
		List<DayChecklistResult> dayResults
	) {
	}

	public record DayChecklistResult(UUID itineraryDayId, UUID checklistId, int itemCount) {
	}
}
