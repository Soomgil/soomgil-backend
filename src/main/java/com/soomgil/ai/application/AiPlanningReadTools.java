package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.ListChecklistsQueryHandler;
import com.soomgil.planning.application.query.GetNoteQuery;
import com.soomgil.planning.application.query.ListChecklistsQuery;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 공동 메모와 체크리스트 조회 도구.
 *
 * <p>쓰기 도구와 분리해 두어 조회만 필요한 요청에서 변경 권한 없이 사용할 수 있다.
 */
public final class AiPlanningReadTools extends AiToolSupport {

	private final ListChecklistsQueryHandler checklistsHandler;
	private final GetNoteQueryHandler noteHandler;

	AiPlanningReadTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		ListChecklistsQueryHandler checklistsHandler,
		GetNoteQueryHandler noteHandler
	) {
		super(request, auditService);
		this.checklistsHandler = checklistsHandler;
		this.noteHandler = noteHandler;
	}

	@Tool(description = "여행방의 체크리스트와 항목을 조회한다. "
		+ "\"체크리스트 뭐 있어\", \"준비물 다 챙겼나\", \"남은 할 일 알려줘\"처럼 "
		+ "이미 저장된 체크리스트 내용을 묻는 요청에 사용한다. "
		+ "itineraryDayId를 주면 해당 일차 체크리스트만, null이면 여행방 전체를 조회한다.")
	public Object getChecklists(ChecklistScopeInput input) {
		UUID dayId = input == null ? null : input.itineraryDayId();
		return execute("getChecklists", AiToolExecutionPolicy.READ, input, null,
			() -> checklistsHandler.handle(new ListChecklistsQuery(
				tripId,
				dayId == null ? null : PlanningScopeType.DAY,
				dayId,
				userId
			)));
	}

	@Tool(description = "여행방의 공동 메모 내용을 조회한다. "
		+ "\"메모에 뭐라고 썼지\", \"메모 보여줘\"처럼 저장된 메모를 묻는 요청에 사용한다. "
		+ "itineraryDayId를 주면 해당 일차 메모를, null이면 여행방 전체 메모를 조회한다.")
	public Object getNote(NoteScopeInput input) {
		UUID dayId = input == null ? null : input.itineraryDayId();
		return execute("getNote", AiToolExecutionPolicy.READ, input, null,
			() -> noteHandler.handle(new GetNoteQuery(
				tripId,
				dayId == null ? PlanningScopeType.TRIP : PlanningScopeType.DAY,
				dayId,
				userId
			)));
	}

	/** @param itineraryDayId 일차 체크리스트만 조회할 때의 일차 ID. null이면 여행방 전체 */
	public record ChecklistScopeInput(UUID itineraryDayId) {
	}

	/** @param itineraryDayId 일차 메모를 조회할 때의 일차 ID. null이면 여행방 전체 메모 */
	public record NoteScopeInput(UUID itineraryDayId) {
	}
}
