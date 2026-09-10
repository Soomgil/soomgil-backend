package com.soomgil.itinerary.api;

import com.soomgil.collaboration.api.dto.VersionedCommandRequest;
import com.soomgil.collaboration.infrastructure.web.HttpCollaborationSessionIdProvider;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.api.dto.CreateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.DeleteMapDrawingsRequest;
import com.soomgil.itinerary.api.dto.ItineraryMutationResponse;
import com.soomgil.itinerary.api.dto.MapDrawing;
import com.soomgil.itinerary.api.dto.UpdateMapDrawingRequest;
import com.soomgil.itinerary.application.command.dto.CreateMapDrawingCommand;
import com.soomgil.itinerary.application.command.dto.DeleteMapDrawingCommand;
import com.soomgil.itinerary.application.command.dto.DeleteMapDrawingsCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.MapDrawingView;
import com.soomgil.itinerary.application.command.dto.UpdateMapDrawingCommand;
import com.soomgil.itinerary.application.command.handler.CreateMapDrawingHandler;
import com.soomgil.itinerary.application.command.handler.DeleteMapDrawingHandler;
import com.soomgil.itinerary.application.command.handler.UpdateMapDrawingHandler;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 지도 drawing과 이미지·스티커 오브젝트의 영구 상태를 REST로 변경한다. */
@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/map-drawings")
public class MapDrawingController {

	private final CreateMapDrawingHandler createHandler;
	private final UpdateMapDrawingHandler updateHandler;
	private final DeleteMapDrawingHandler deleteHandler;
	private final HttpCollaborationSessionIdProvider sessionIdProvider;

	public MapDrawingController(
		CreateMapDrawingHandler createHandler,
		UpdateMapDrawingHandler updateHandler,
		DeleteMapDrawingHandler deleteHandler,
		HttpCollaborationSessionIdProvider sessionIdProvider
	) {
		this.createHandler = createHandler;
		this.updateHandler = updateHandler;
		this.deleteHandler = deleteHandler;
		this.sessionIdProvider = sessionIdProvider;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ItineraryMutationResponse create(
		@PathVariable UUID tripId,
		@RequestHeader(name = HttpCollaborationSessionIdProvider.SESSION_HEADER) String websocketSessionId,
		@Valid @RequestBody CreateMapDrawingRequest request,
		Principal principal
	) {
		UUID userId = currentUserId(principal);
		return toResponse(createHandler.handle(new CreateMapDrawingCommand(
			tripId,
			userId,
			request.baseVersion(),
			request.itineraryDayId(),
			com.soomgil.itinerary.domain.model.DrawingType.valueOf(request.drawingType().name()),
			request.geometry(),
			request.style(),
			request.label(),
			request.mediaFileId(),
			request.stickerCode(),
			request.transform(),
			request.sortOrder(),
			sessionIdProvider.requireOwnedSession(websocketSessionId, principal)
		)));
	}

	@PatchMapping("/{drawingId}")
	public ItineraryMutationResponse update(
		@PathVariable UUID tripId,
		@PathVariable UUID drawingId,
		@RequestHeader(name = HttpCollaborationSessionIdProvider.SESSION_HEADER) String websocketSessionId,
		@Valid @RequestBody UpdateMapDrawingRequest request,
		Principal principal
	) {
		UUID userId = currentUserId(principal);
		return toResponse(updateHandler.handle(new UpdateMapDrawingCommand(
			tripId,
			userId,
			request.baseVersion(),
			drawingId,
			request.geometry(),
			request.style(),
			request.label(),
			request.transform(),
			request.sortOrder(),
			request.drawingVersion(),
			sessionIdProvider.requireOwnedSession(websocketSessionId, principal)
		)));
	}

	@DeleteMapping("/{drawingId}")
	public ItineraryMutationResponse delete(
		@PathVariable UUID tripId,
		@PathVariable UUID drawingId,
		@RequestHeader(name = HttpCollaborationSessionIdProvider.SESSION_HEADER) String websocketSessionId,
		@Valid @RequestBody VersionedCommandRequest request,
		Principal principal
	) {
		UUID userId = currentUserId(principal);
		return toResponse(deleteHandler.handle(new DeleteMapDrawingCommand(
			tripId,
			userId,
			request.baseVersion(),
			drawingId,
			sessionIdProvider.requireOwnedSession(websocketSessionId, principal)
		)));
	}

	@PostMapping("/batch-delete")
	public ItineraryMutationResponse deleteBatch(
		@PathVariable UUID tripId,
		@RequestHeader(name = HttpCollaborationSessionIdProvider.SESSION_HEADER) String websocketSessionId,
		@Valid @RequestBody DeleteMapDrawingsRequest request,
		Principal principal
	) {
		UUID userId = currentUserId(principal);
		return toResponse(deleteHandler.handle(new DeleteMapDrawingsCommand(
			tripId,
			userId,
			request.baseVersion(),
			request.drawingIds(),
			sessionIdProvider.requireOwnedSession(websocketSessionId, principal)
		)));
	}

	private ItineraryMutationResponse toResponse(ItineraryMutationResult result) {
		return new ItineraryMutationResponse(
			result.tripId(), result.itineraryVersion(), null, null, null,
			result.drawing() == null ? null : toDrawing(result.drawing()), result.affectedRouteIds()
		);
	}

	private MapDrawing toDrawing(MapDrawingView view) {
		return new MapDrawing(
			view.id(), view.itineraryDayId(),
			com.soomgil.itinerary.api.dto.DrawingType.valueOf(view.drawingType().name()),
			com.soomgil.itinerary.api.dto.GeometryFormat.valueOf(view.geometryFormat().name()),
			view.geometry(), view.style(), view.label(), view.mediaFileId(), view.stickerCode(), view.transform(),
			view.sortOrder(), view.version()
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
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user id must be a UUID.");
		}
	}
}
