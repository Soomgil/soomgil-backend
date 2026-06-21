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
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
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
			mock(UpsertNoteCommandHandler.class), mock(UpsertChecklistCommandHandler.class),
			mock(CreateItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			new AiGuideRequest(tripId, userId, UUID.randomUUID(), UUID.randomUUID(), null, java.util.List.of(), "질문", null, null),
			mock(AiToolAuditService.class)
		);

		tools.getCurrentItinerary();

		verify(itineraryHandler).handle(new FindItineraryQuery(tripId, userId));
	}
}
