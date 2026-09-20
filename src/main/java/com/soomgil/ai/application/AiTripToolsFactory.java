package com.soomgil.ai.application;

import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.ListChecklistsQueryHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * intent별로 LLM에 노출할 도구 묶음을 만든다.
 *
 * <p>각 묶음의 <b>첫 번째 도구가 그 intent의 주 도구</b>다. LLM 호출이 실패했을 때
 * {@link LocalFallbackAiGuideModel}이 첫 번째 도구만 꺼내 결정적으로 실행하므로 순서를 바꾸지 않는다.
 *
 * <p>쓰기 intent에는 주 도구 뒤에 조회 도구를 함께 붙인다. 장소를 추가하거나 옮기려면 현재 일정을
 * 먼저 확인해야 하는데, 도구를 하나만 주면 모델이 맥락 JSON만 보고 추측하게 되어 실패율이 높았다.
 */
@Component
public class AiTripToolsFactory {

	private final FindItineraryHandler itineraryHandler;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final ListPlaceRecommendationsQueryHandler recommendationHandler;
	private final UpsertNoteCommandHandler noteHandler;
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateChecklistItemCommandHandler checklistItemHandler;
	private final ListChecklistsQueryHandler listChecklistsHandler;
	private final GetNoteQueryHandler getNoteHandler;
	private final AiItineraryToolService itineraryToolService;
	private final AiToolAuditService auditService;
	private final AiRecommendationViewportResolver viewportResolver;

	public AiTripToolsFactory(
		FindItineraryHandler itineraryHandler,
		PlaceSearchQueryHandler placeSearchHandler,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		UpsertNoteCommandHandler noteHandler,
		UpsertChecklistCommandHandler checklistHandler,
		CreateChecklistItemCommandHandler checklistItemHandler,
		ListChecklistsQueryHandler listChecklistsHandler,
		GetNoteQueryHandler getNoteHandler,
		AiItineraryToolService itineraryToolService,
		AiToolAuditService auditService,
		AiRecommendationViewportResolver viewportResolver
	) {
		this.itineraryHandler = itineraryHandler;
		this.placeSearchHandler = placeSearchHandler;
		this.recommendationHandler = recommendationHandler;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.checklistItemHandler = checklistItemHandler;
		this.listChecklistsHandler = listChecklistsHandler;
		this.getNoteHandler = getNoteHandler;
		this.itineraryToolService = itineraryToolService;
		this.auditService = auditService;
		this.viewportResolver = viewportResolver;
	}

	public List<AiExecutableTools> create(AiGuideRequest request, AiIntent intent) {
		return switch (intent) {
			case READ_ITINERARY -> List.of(
				itineraryRead(request),
				planningRead(request)
			);
			case READ_PLANNING -> List.of(
				planningRead(request),
				itineraryRead(request)
			);
			case SEARCH_PLACES -> List.of(
				new AiPlaceSearchTools(request, auditService, placeSearchHandler)
			);
			case RECOMMEND_PLACES -> List.of(
				new AiPlaceRecommendationTools(request, auditService, recommendationHandler, viewportResolver)
			);
			case SUMMARIZE_ITINERARY -> List.of(
				new AiSummarizeItineraryTools(request, auditService, itineraryHandler),
				planningRead(request)
			);
			case WRITE_NOTE -> List.of(
				new AiNoteTools(request, auditService, noteHandler, getNoteHandler),
				itineraryRead(request)
			);
			case WRITE_CHECKLIST -> List.of(
				new AiChecklistTools(request, auditService, checklistHandler, checklistItemHandler),
				planningRead(request),
				itineraryRead(request)
			);
			case ADD_PLACE_TO_ITINERARY -> List.of(
				new AiAddPlaceTools(request, auditService, itineraryToolService),
				new AiPlaceSearchTools(request, auditService, placeSearchHandler),
				itineraryRead(request),
				// 장소 이름이 없거나 검색 결과가 비었을 때 되묻지 않고 취향 기반 추천으로 채울 수 있게 함께 노출한다.
				new AiAddRecommendedPlacesTools(request, auditService, recommendationHandler, itineraryToolService, viewportResolver)
			);
			case ADD_RECOMMENDED_PLACES_TO_ITINERARY -> List.of(
				new AiAddRecommendedPlacesTools(request, auditService, recommendationHandler, itineraryToolService, viewportResolver),
				itineraryRead(request)
			);
			case DELETE_ITINERARY_ITEM -> List.of(
				new AiDeleteItineraryItemTools(request, auditService, itineraryToolService),
				itineraryRead(request)
			);
			case MOVE_ITINERARY_ITEM -> List.of(
				new AiMoveItineraryItemTools(request, auditService, itineraryToolService),
				itineraryRead(request)
			);
			case FILTER_PLACES_BY_CONDITION -> List.of(
				new AiFilterPlacesTools(request, auditService, itineraryToolService),
				itineraryRead(request)
			);
			case GENERATE_CHECKLIST_FROM_ITINERARY -> List.of(
				new AiGenerateChecklistTools(request, auditService, checklistHandler, checklistItemHandler),
				itineraryRead(request),
				planningRead(request)
			);
			case OPTIMIZE_ROUTE, CONNECT_DAY_ROUTES -> List.of(
				new AiOptimizeRouteTools(request, auditService, itineraryToolService),
				itineraryRead(request)
			);
			case MANAGE_ITINERARY_DAY -> List.of(
				new AiItineraryDayTools(request, auditService, itineraryToolService),
				itineraryRead(request)
			);
			default -> List.of();
		};
	}

	private AiItineraryReadTools itineraryRead(AiGuideRequest request) {
		return new AiItineraryReadTools(request, auditService, itineraryHandler);
	}

	private AiPlanningReadTools planningRead(AiGuideRequest request) {
		return new AiPlanningReadTools(request, auditService, listChecklistsHandler, getNoteHandler);
	}
}
