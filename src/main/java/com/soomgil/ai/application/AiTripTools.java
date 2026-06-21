package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.ai.tool.annotation.Tool;

public class AiTripTools {

	private final UUID tripId;
	private final UUID userId;
	private final FindItineraryHandler itineraryHandler;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final ListPlaceRecommendationsQueryHandler recommendationHandler;
	private final UpsertNoteCommandHandler noteHandler;
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateChecklistItemCommandHandler checklistItemHandler;
	private final AiItineraryToolService itineraryToolService;
	private final UpdateItineraryItemHandler updateItemHandler;
	private final AiGuideRequest request;
	private final AiToolAuditService auditService;
	private final List<AiToolCall> executedCalls = new ArrayList<>();

	public AiTripTools(
		UUID tripId,
		UUID userId,
		FindItineraryHandler itineraryHandler,
		PlaceSearchQueryHandler placeSearchHandler,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		UpsertNoteCommandHandler noteHandler,
		UpsertChecklistCommandHandler checklistHandler,
		CreateChecklistItemCommandHandler checklistItemHandler,
		AiItineraryToolService itineraryToolService,
		UpdateItineraryItemHandler updateItemHandler,
		AiGuideRequest request,
		AiToolAuditService auditService
	) {
		this.tripId = tripId;
		this.userId = userId;
		this.itineraryHandler = itineraryHandler;
		this.placeSearchHandler = placeSearchHandler;
		this.recommendationHandler = recommendationHandler;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.checklistItemHandler = checklistItemHandler;
		this.itineraryToolService = itineraryToolService;
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

	@Tool(description = "현재 여행방 멤버들의 취향을 종합해 지도 범위 안의 장소를 추천한다. 원시 취향 점수 대신 공개 가능한 매칭 멤버와 추천 이유만 반환한다")
	public Object recommendPlaces(RecommendPlacesInput input) {
		String bbox = bbox(input.bbox());
		Double centerLat = input.centerLat() != null ? input.centerLat() : viewportCenterLat();
		Double centerLng = input.centerLng() != null ? input.centerLng() : viewportCenterLng();
		RecommendationTab tab = input.tab() == null || input.tab().isBlank()
			? RecommendationTab.BASIC
			: RecommendationTab.valueOf(input.tab().trim().toUpperCase());
		int size = Math.max(1, Math.min(input.size() == null ? 10 : input.size(), 10));
		ListPlaceRecommendationsQuery query = new ListPlaceRecommendationsQuery(
			tripId, bbox, centerLat, centerLng, tab, 0, size
		);
		return execute("recommendPlaces", AiToolExecutionPolicy.READ, input, null,
			() -> recommendationHandler.handle(query));
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

	@Tool(description = "기존 공동 체크리스트에 준비물이나 할 일 항목 하나를 추가한다")
	public Object addChecklistItem(ChecklistItemInput input) {
		return execute("addChecklistItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, null,
			() -> checklistItemHandler.handle(new CreateChecklistItemCommand(
				tripId, input.checklistId(), userId, input.content(), input.sortOrder()
			)));
	}

	@Tool(description = "사용자가 명시적으로 추가를 요청한 장소 하나를 지정한 일정 일차에 추가한다")
	public Object addPlaceToItinerary(AddPlaceInput input) {
		long baseVersion = baseVersion(input.baseVersion());
		return execute("addPlaceToItinerary", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, baseVersion,
			() -> itineraryToolService.addPlace(
				tripId, userId, baseVersion, new AiItineraryToolService.AddPlaceInput(
					input.itineraryDayId(), input.sortOrder(), input.placeProvider(), input.externalPlaceId(),
					input.placeName(), input.address(), input.lat(), input.lng(), uri(input.thumbnailUrl())
				)
			));
	}

	@Tool(description = "기존 일정 항목을 다른 일차 또는 순서로 이동한다")
	public Object moveItineraryItem(MoveItemInput input) {
		long baseVersion = baseVersion(input.baseVersion());
		return execute("moveItineraryItem", AiToolExecutionPolicy.REVERSIBLE_WRITE, input, baseVersion, () -> updateItemHandler.handle(new UpdateItineraryItemCommand(
			tripId, userId, baseVersion, input.itemId(), input.itineraryDayId(), input.sortOrder(),
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

	private long baseVersion(Long supplied) {
		if (supplied != null) {
			return supplied;
		}
		if (request.baseVersion() != null) {
			return request.baseVersion();
		}
		if (request.tripContext() != null && request.tripContext().trip() != null) {
			return request.tripContext().trip().itineraryVersion();
		}
		throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary baseVersion is required.");
	}

	private String bbox(String supplied) {
		if (supplied != null && !supplied.isBlank()) {
			return supplied;
		}
		if (request.viewport() == null || request.viewport().minLng() == null
			|| request.viewport().minLat() == null || request.viewport().maxLng() == null
			|| request.viewport().maxLat() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Recommendation viewport is required.");
		}
		return request.viewport().minLng() + "," + request.viewport().minLat() + ","
			+ request.viewport().maxLng() + "," + request.viewport().maxLat();
	}

	private Double viewportCenterLat() {
		return request.viewport() == null || request.viewport().minLat() == null || request.viewport().maxLat() == null
			? null : (request.viewport().minLat() + request.viewport().maxLat()) / 2;
	}

	private Double viewportCenterLng() {
		return request.viewport() == null || request.viewport().minLng() == null || request.viewport().maxLng() == null
			? null : (request.viewport().minLng() + request.viewport().maxLng()) / 2;
	}

	public record SearchPlacesInput(String query, String bbox, String legalRegionCode, String category) {
	}

	public record RecommendPlacesInput(
		String bbox, Double centerLat, Double centerLng, String tab, Integer size
	) {
	}

	public record ScopedTextInput(String scope, UUID itineraryDayId, String text) {
	}

	public record ChecklistItemInput(UUID checklistId, String content, Integer sortOrder) {
	}

	public record AddPlaceInput(
		Long baseVersion, UUID itineraryDayId, int sortOrder, String placeProvider, String externalPlaceId,
		String placeName, String address, Double lat, Double lng, String thumbnailUrl
	) {
	}

	public record MoveItemInput(Long baseVersion, UUID itemId, UUID itineraryDayId, Integer sortOrder) {
	}
}
