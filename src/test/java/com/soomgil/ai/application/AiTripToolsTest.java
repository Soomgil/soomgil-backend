package com.soomgil.ai.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiTripToolsTest {

	@Test
	void readsTheItineraryAsTheRequestingTripMember() {
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(mock(ItineraryView.class));
		AiTripTools tools = new AiTripTools(
			tripId, userId, itineraryHandler, mock(PlaceSearchQueryHandler.class),
			mock(ListPlaceRecommendationsQueryHandler.class),
			mock(UpsertNoteCommandHandler.class), mock(UpsertChecklistCommandHandler.class),
			mock(CreateChecklistItemCommandHandler.class),
			mock(AiItineraryToolService.class),
			mock(UpdateItineraryItemHandler.class),
			new AiGuideRequest(tripId, userId, UUID.randomUUID(), UUID.randomUUID(), null, java.util.List.of(), "질문", null, null, null),
			mock(AiToolAuditService.class)
		);

		tools.getCurrentItinerary();

		verify(itineraryHandler).handle(new FindItineraryQuery(tripId, userId));
	}

	@Test
	void recommendsPlacesThroughTheExistingGroupRecommendationQuery() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		ListPlaceRecommendationsQueryHandler recommendationHandler = mock(ListPlaceRecommendationsQueryHandler.class);
		AiTripTools tools = tools(tripId, userId, recommendationHandler, mock(CreateChecklistItemCommandHandler.class));

		tools.recommendPlaces(new AiTripTools.RecommendPlacesInput(
			"126.8,37.4,127.2,37.7", 37.55, 127.0, "BASIC", 5
		));

		verify(recommendationHandler).handle(new ListPlaceRecommendationsQuery(
			tripId, "126.8,37.4,127.2,37.7", 37.55, 127.0, RecommendationTab.BASIC, 0, 5
		));
	}

	@Test
	void addsChecklistItemsThroughThePlanningCommandHandler() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		CreateChecklistItemCommandHandler itemHandler = mock(CreateChecklistItemCommandHandler.class);
		AiTripTools tools = tools(tripId, userId, mock(ListPlaceRecommendationsQueryHandler.class), itemHandler);

		tools.addChecklistItem(new AiTripTools.ChecklistItemInput(checklistId, "여권 챙기기", null));

		verify(itemHandler).handle(new com.soomgil.planning.application.command.CreateChecklistItemCommand(
			tripId, checklistId, userId, "여권 챙기기", null
		));
	}

	private AiTripTools tools(
		UUID tripId,
		UUID userId,
		ListPlaceRecommendationsQueryHandler recommendationHandler,
		CreateChecklistItemCommandHandler itemHandler
	) {
		return new AiTripTools(
			tripId, userId, mock(FindItineraryHandler.class), mock(PlaceSearchQueryHandler.class),
			recommendationHandler, mock(UpsertNoteCommandHandler.class),
			mock(UpsertChecklistCommandHandler.class), itemHandler,
			mock(AiItineraryToolService.class),
			mock(UpdateItineraryItemHandler.class),
			new AiGuideRequest(tripId, userId, UUID.randomUUID(), UUID.randomUUID(), null,
				java.util.List.of(), "질문", null, null, null),
			mock(AiToolAuditService.class)
		);
	}
}
