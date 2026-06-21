package com.soomgil.ai.application;

import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import java.net.URI;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import org.springframework.ai.tool.annotation.Tool;

public class AiTripTools {

	private final UUID tripId;
	private final UUID userId;
	private final FindItineraryHandler itineraryHandler;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final UpsertNoteCommandHandler noteHandler;
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateItineraryItemHandler createItemHandler;
	private final UpdateItineraryItemHandler updateItemHandler;
	private final AiGuideRequest request;
	private final AiToolAuditService auditService;
	private final List<AiToolCall> executedCalls = new ArrayList<>();

	public AiTripTools(
		UUID tripId,
		UUID userId,
		FindItineraryHandler itineraryHandler,
		PlaceSearchQueryHandler placeSearchHandler,
		UpsertNoteCommandHandler noteHandler,
		UpsertChecklistCommandHandler checklistHandler,
		CreateItineraryItemHandler createItemHandler,
		UpdateItineraryItemHandler updateItemHandler,
		AiGuideRequest request,
		AiToolAuditService auditService
	) {
		this.tripId = tripId;
		this.userId = userId;
		this.itineraryHandler = itineraryHandler;
		this.placeSearchHandler = placeSearchHandler;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.createItemHandler = createItemHandler;
		this.updateItemHandler = updateItemHandler;
		this.request = request;
		this.auditService = auditService;
	}

	@Tool(description = "현재 여행방의 일차, 일정 장소, 경로를 조회한다")
	public Object getCurrentItinerary() {
		return execute("getCurrentItinerary", AiToolExecutionPolicy.READ, null, null,
			() -> itineraryHandler.handle(new FindItineraryQuery(tripId, userId)));
	}

	@Tool(description = "한국 관광지 후보를 검색한다. 일정에 추가하지 않고 후보만 반환한다")
	public Object searchPlaces(SearchPlacesInput input) {
		return execute("searchPlaces", AiToolExecutionPolicy.READ, input, null, () -> placeSearchHandler.handle(new PlaceSearchQuery(
			input.query(), input.bbox(), input.legalRegionCode(), input.category(), 0, 10
		)));
	}

	@Tool(description = "여행방 전체 또는 특정 일차의 공동 메모를 작성하거나 수정한다")
	public Object upsertNote(ScopedTextInput input) {
		return execute("upsertNote", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null, () -> noteHandler.handle(new UpsertNoteCommand(
			tripId, userId, scope(input.scope()), input.itineraryDayId(), input.text()
		)));
	}

	@Tool(description = "여행방 전체 또는 특정 일차의 공동 체크리스트를 생성하거나 제목을 수정한다")
	public Object upsertChecklist(ScopedTextInput input) {
		return execute("upsertChecklist", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null, () -> checklistHandler.handle(new UpsertChecklistCommand(
			tripId, userId, scope(input.scope()), input.itineraryDayId(), input.text()
		)));
	}

	@Tool(description = "사용자가 명시적으로 추가를 요청한 장소 하나를 지정한 일정 일차에 추가한다")
	public Object addPlaceToItinerary(AddPlaceInput input) {
		return execute("addPlaceToItinerary", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, input.baseVersion(), () -> createItemHandler.handle(new CreateItineraryItemCommand(
			tripId, userId, input.baseVersion(), input.itineraryDayId(), input.sortOrder(),
			ItineraryItemType.PLACE, input.placeProvider(), input.externalPlaceId(), input.placeName(),
			input.address(), input.lat(), input.lng(), uri(input.thumbnailUrl())
		)));
	}

	@Tool(description = "기존 일정 항목을 다른 일차 또는 순서로 이동한다")
	public Object moveItineraryItem(MoveItemInput input) {
		return execute("moveItineraryItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, input.baseVersion(), () -> updateItemHandler.handle(new UpdateItineraryItemCommand(
			tripId, userId, input.baseVersion(), input.itemId(), input.itineraryDayId(), input.sortOrder(),
			null, null, null, null, null
		)));
	}

	public List<AiToolCall> executedCalls() {
		return List.copyOf(executedCalls);
	}

	private Object execute(
		String toolName,
		AiToolExecutionPolicy policy,
		Object arguments,
		Long versionBefore,
		Supplier<Object> action
	) {
		UUID callId = auditService.start(request, toolName, policy, arguments, versionBefore);
		try {
			Object result = action.get();
			Long versionAfter = versionAfter(result);
			boolean undoAvailable = result instanceof ItineraryMutationResult
				&& auditService.hasCollaborationSession();
			AiToolCall call = auditService.succeed(
				callId, toolName, policy, result, versionBefore, versionAfter,
				undoAvailable
			);
			executedCalls.add(call);
			return result;
		}
		catch (RuntimeException exception) {
			auditService.fail(callId, exception);
			throw exception;
		}
	}

	private Long versionAfter(Object result) {
		if (result instanceof ItineraryMutationResult itinerary) {
			return itinerary.itineraryVersion();
		}
		if (result instanceof PlanningMutationResponse planning) {
			return planning.itineraryVersion();
		}
		return null;
	}

	private PlanningScopeType scope(String value) {
		return PlanningScopeType.valueOf(value.trim().toUpperCase());
	}

	private URI uri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}

	public record SearchPlacesInput(String query, String bbox, String legalRegionCode, String category) {
	}

	public record ScopedTextInput(String scope, UUID itineraryDayId, String text) {
	}

	public record AddPlaceInput(
		long baseVersion, UUID itineraryDayId, int sortOrder, String placeProvider, String externalPlaceId,
		String placeName, String address, Double lat, Double lng, String thumbnailUrl
	) {
	}

	public record MoveItemInput(long baseVersion, UUID itemId, UUID itineraryDayId, Integer sortOrder) {
	}
}
