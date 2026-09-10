package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.planning.application.query.GetNoteQuery;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

public final class AiNoteTools extends AiToolSupport {
	private final UpsertNoteCommandHandler noteHandler;
	private final GetNoteQueryHandler noteQueryHandler;

	AiNoteTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		UpsertNoteCommandHandler noteHandler,
		GetNoteQueryHandler noteQueryHandler
	) {
		super(request, auditService);
		this.noteHandler = noteHandler;
		this.noteQueryHandler = noteQueryHandler;
	}

	@Tool(description = "여행방 전체 또는 특정 일차의 공동 메모를 작성하거나 수정한다")
	public Object upsertNote(ScopedTextInput input) {
		PlanningScopeType scopeType = PlanningScopeType.valueOf(input.scope().trim().toUpperCase());
		return execute("upsertNote", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null,
			() -> {
				long baseVersion = noteQueryHandler.findOptional(new GetNoteQuery(
					tripId, scopeType, input.itineraryDayId(), userId
				)).map(note -> note.version()).orElse(0L);
				return noteHandler.handle(new UpsertNoteCommand(
					tripId, userId, scopeType, input.itineraryDayId(), input.text(), baseVersion
				));
			});
	}

	public record ScopedTextInput(String scope, UUID itineraryDayId, String text) {
	}
}
