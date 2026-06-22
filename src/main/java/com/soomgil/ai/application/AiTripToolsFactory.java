package com.soomgil.ai.application;

import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AiTripToolsFactory {

	private final FindItineraryHandler itineraryHandler;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final ListPlaceRecommendationsQueryHandler recommendationHandler;
	private final UpsertNoteCommandHandler noteHandler;
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateChecklistItemCommandHandler checklistItemHandler;
	private final AiItineraryToolService itineraryToolService;
	private final AiToolAuditService auditService;

	public AiTripToolsFactory(
		FindItineraryHandler itineraryHandler,
		PlaceSearchQueryHandler placeSearchHandler,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		UpsertNoteCommandHandler noteHandler,
		UpsertChecklistCommandHandler checklistHandler,
		CreateChecklistItemCommandHandler checklistItemHandler,
		AiItineraryToolService itineraryToolService,
		AiToolAuditService auditService
	) {
		this.itineraryHandler = itineraryHandler;
		this.placeSearchHandler = placeSearchHandler;
		this.recommendationHandler = recommendationHandler;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.checklistItemHandler = checklistItemHandler;
		this.itineraryToolService = itineraryToolService;
		this.auditService = auditService;
	}

	public List<AiExecutableTools> create(AiGuideRequest request, AiIntent intent) {
		return switch (intent) {
			case READ_ITINERARY -> List.of(new AiItineraryReadTools(request, auditService, itineraryHandler));
			case SEARCH_PLACES -> List.of(new AiPlaceSearchTools(request, auditService, placeSearchHandler));
			case RECOMMEND_PLACES -> List.of(new AiPlaceRecommendationTools(
				request, auditService, recommendationHandler
			));
			case WRITE_NOTE -> List.of(new AiNoteTools(request, auditService, noteHandler));
			case WRITE_CHECKLIST -> List.of(new AiChecklistTools(
				request, auditService, checklistHandler, checklistItemHandler
			));
			case ADD_PLACE_TO_ITINERARY -> List.of(new AiAddPlaceTools(
				request, auditService, itineraryToolService
			));
			case DELETE_ITINERARY_ITEM -> List.of(new AiDeleteItineraryItemTools(
				request, auditService, itineraryToolService
			));
			case MOVE_ITINERARY_ITEM -> List.of(new AiMoveItineraryItemTools(
				request, auditService, itineraryToolService
			));
			case SUMMARIZE_ITINERARY -> List.of(new AiSummarizeItineraryTools(
				request, auditService, itineraryHandler
			));
			case FILTER_PLACES_BY_CONDITION -> List.of(new AiFilterPlacesTools(
				request, auditService, itineraryToolService
			));
			case GENERATE_CHECKLIST_FROM_ITINERARY -> List.of(new AiGenerateChecklistTools(
				request, auditService, checklistHandler, checklistItemHandler
			));
			case OPTIMIZE_ROUTE -> List.of(new AiOptimizeRouteTools(
				request, auditService, itineraryToolService
			));
			default -> List.of();
		};
	}
}
