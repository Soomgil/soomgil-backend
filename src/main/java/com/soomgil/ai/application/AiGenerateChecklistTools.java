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
		+ "대상 checklist가 있으면 checklistId를 쓰고, 없으면 checklistId를 null로 두면 scope/dayId로 새로 만든다.")
	@Transactional
	public Object generateChecklistItems(GenerateItemsInput input) {
		PlanningMutationResponse response = buildChecklistItems(input);
		return execute(
			"generateChecklistItems",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, null,
			() -> response
		);
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
}
