package com.soomgil.ai.application;

import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import org.springframework.stereotype.Component;

@Component
public class AiTripToolsFactory {

	private final FindItineraryHandler itineraryHandler;
	private final PlaceSearchQueryHandler placeSearchHandler;
	private final UpsertNoteCommandHandler noteHandler;
	private final UpsertChecklistCommandHandler checklistHandler;
	private final CreateItineraryItemHandler createItemHandler;
	private final UpdateItineraryItemHandler updateItemHandler;
	private final AiToolAuditService auditService;

	public AiTripToolsFactory(
		FindItineraryHandler itineraryHandler,
		PlaceSearchQueryHandler placeSearchHandler,
		UpsertNoteCommandHandler noteHandler,
		UpsertChecklistCommandHandler checklistHandler,
		CreateItineraryItemHandler createItemHandler,
		UpdateItineraryItemHandler updateItemHandler,
		AiToolAuditService auditService
	) {
		this.itineraryHandler = itineraryHandler;
		this.placeSearchHandler = placeSearchHandler;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.createItemHandler = createItemHandler;
		this.updateItemHandler = updateItemHandler;
		this.auditService = auditService;
	}

	public AiTripTools create(AiGuideRequest request) {
		return new AiTripTools(
			request.tripId(), request.requesterUserId(), itineraryHandler, placeSearchHandler,
			noteHandler, checklistHandler, createItemHandler, updateItemHandler, request, auditService
		);
	}
}
