package com.soomgil.collaboration.api;

import com.soomgil.collaboration.api.dto.CollaborationActionResponse;
import com.soomgil.collaboration.api.dto.UndoRedoRequest;
import com.soomgil.collaboration.application.command.dto.UndoRedoAction;
import com.soomgil.collaboration.application.command.dto.UndoRedoCommand;
import com.soomgil.collaboration.application.command.dto.UndoRedoResult;
import com.soomgil.collaboration.application.command.handler.UndoRedoHandler;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/collaboration")
public class CollaborationController extends ApiControllerSupport {

	private static final String WEBSOCKET_SESSION_HEADER = "X-Soomgil-WebSocket-Session-Id";

	private final UndoRedoHandler undoRedoHandler;

	public CollaborationController(UndoRedoHandler undoRedoHandler) {
		this.undoRedoHandler = Objects.requireNonNull(undoRedoHandler, "undoRedoHandler must not be null");
	}

	@PostMapping("/undo")
	public CollaborationActionResponse undo(
		@PathVariable UUID tripId,
		@Valid @RequestBody UndoRedoRequest request,
		@RequestHeader(name = WEBSOCKET_SESSION_HEADER, required = false) String websocketSessionId,
		Principal principal
	) {
		return toResponse(undoRedoHandler.handle(new UndoRedoCommand(
			tripId,
			currentUserId(principal),
			normalizeSessionId(websocketSessionId),
			request.baseVersion(),
			request.commandEventId(),
			UndoRedoAction.UNDO
		)));
	}

	@PostMapping("/redo")
	public CollaborationActionResponse redo(
		@PathVariable UUID tripId,
		@Valid @RequestBody UndoRedoRequest request,
		@RequestHeader(name = WEBSOCKET_SESSION_HEADER, required = false) String websocketSessionId,
		Principal principal
	) {
		return toResponse(undoRedoHandler.handle(new UndoRedoCommand(
			tripId,
			currentUserId(principal),
			normalizeSessionId(websocketSessionId),
			request.baseVersion(),
			request.commandEventId(),
			UndoRedoAction.REDO
		)));
	}

	private CollaborationActionResponse toResponse(UndoRedoResult result) {
		return new CollaborationActionResponse(
			result.tripId(),
			result.itineraryVersion(),
			result.commandEventId(),
			result.undoAvailable(),
			result.redoAvailable()
		);
	}

	private UUID currentUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is required.");
		}
		try {
			return Ids.parseUuid(principal.getName(), "currentUserId");
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is invalid.");
		}
	}

	private String normalizeSessionId(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
