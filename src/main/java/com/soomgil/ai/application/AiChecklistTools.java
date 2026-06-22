package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

public final class AiChecklistTools extends AiToolSupport {
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateChecklistItemCommandHandler checklistItemHandler;

	AiChecklistTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		UpsertChecklistCommandHandler checklistHandler,
		CreateChecklistItemCommandHandler checklistItemHandler
	) {
		super(request, auditService);
		this.checklistHandler = checklistHandler;
		this.checklistItemHandler = checklistItemHandler;
	}

	@Tool(description = "여행방 전체 또는 특정 일차의 공동 체크리스트를 생성하거나 제목을 수정한다")
	public Object upsertChecklist(ScopedTextInput input) {
		return execute("upsertChecklist", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null,
			() -> checklistHandler.handle(new UpsertChecklistCommand(
				tripId, userId, PlanningScopeType.valueOf(input.scope().trim().toUpperCase()),
				input.itineraryDayId(), input.text()
			)));
	}

	@Tool(description = "기존 공동 체크리스트에 준비물이나 할 일 항목 하나를 추가한다")
	public Object addChecklistItem(ChecklistItemInput input) {
		return execute("addChecklistItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null,
			() -> checklistItemHandler.handle(new CreateChecklistItemCommand(
				tripId, input.checklistId(), userId, input.content(), input.sortOrder()
			)));
	}

	public record ScopedTextInput(String scope, UUID itineraryDayId, String text) {
	}

	public record ChecklistItemInput(UUID checklistId, String content, Integer sortOrder) {
	}
}
