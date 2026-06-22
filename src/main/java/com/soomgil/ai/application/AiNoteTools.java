package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

public final class AiNoteTools extends AiToolSupport {
	private final UpsertNoteCommandHandler noteHandler;

	AiNoteTools(AiGuideRequest request, AiToolAuditService auditService, UpsertNoteCommandHandler noteHandler) {
		super(request, auditService);
		this.noteHandler = noteHandler;
	}

	@Tool(description = "여행방 전체 또는 특정 일차의 공동 메모를 작성하거나 수정한다")
	public Object upsertNote(ScopedTextInput input) {
		return execute("upsertNote", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null,
			() -> noteHandler.handle(new UpsertNoteCommand(
				tripId, userId, PlanningScopeType.valueOf(input.scope().trim().toUpperCase()),
				input.itineraryDayId(), input.text()
			)));
	}

	public record ScopedTextInput(String scope, UUID itineraryDayId, String text) {
	}
}
