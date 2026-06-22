package com.soomgil.ai.application;

import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import org.springframework.stereotype.Component;

@Component
public class AiTripToolsFactory {

	private final FindItineraryHandler itineraryHandler;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final ListPlaceRecommendationsQueryHandler recommendationHandler;
	private final UpsertNoteCommandHandler noteHandler;
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateChecklistItemCommandHandler checklistItemHandler;
	private final AiItineraryToolService itineraryToolService;
	private final UpdateItineraryItemHandler updateItemHandler;
	private final AiToolAuditService auditService;

	public AiTripToolsFactory(
		FindItineraryHandler itineraryHandler,
		PlaceSearchQueryHandler placeSearchHandler,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		UpsertNoteCommandHandler noteHandler,
		UpsertChecklistCommandHandler checklistHandler,
		CreateChecklistItemCommandHandler checklistItemHandler,
		AiItineraryToolService itineraryToolService,
		UpdateItineraryItemHandler updateItemHandler,
		AiToolAuditService auditService
	) {
		this.itineraryHandler = itineraryHandler;
		this.placeSearchHandler = placeSearchHandler;
		this.recommendationHandler = recommendationHandler;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.checklistItemHandler = checklistItemHandler;
		this.itineraryToolService = itineraryToolService;
		this.updateItemHandler = updateItemHandler;
		this.auditService = auditService;
	}

	public AiTripTools create(AiGuideRequest request) {
		return new AiTripTools(
			request.tripId(), request.requesterUserId(), itineraryHandler, placeSearchHandler,
			recommendationHandler, noteHandler, checklistHandler, checklistItemHandler,
			itineraryToolService, updateItemHandler, request, auditService
		);
	}
}
