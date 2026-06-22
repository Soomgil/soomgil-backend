package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

class AiTripToolsTest {

	@Test
	void readsTheItineraryAsTheRequestingTripMember() {
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(mock(ItineraryView.class));
		AiItineraryReadTools tools = new AiItineraryReadTools(request(tripId, userId), audit(), itineraryHandler);

		tools.getCurrentItinerary();

		verify(itineraryHandler).handle(new FindItineraryQuery(tripId, userId));
	}

	@Test
	void recommendsPlacesThroughTheExistingGroupRecommendationQuery() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		ListPlaceRecommendationsQueryHandler recommendationHandler = mock(ListPlaceRecommendationsQueryHandler.class);
		AiPlaceRecommendationTools tools = new AiPlaceRecommendationTools(
			request(tripId, userId), audit(), recommendationHandler
		);

		tools.recommendPlaces(new AiPlaceRecommendationTools.RecommendPlacesInput(
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
		AiChecklistTools tools = new AiChecklistTools(
			request(tripId, userId), audit(), mock(UpsertChecklistCommandHandler.class), itemHandler
		);

		tools.addChecklistItem(new AiChecklistTools.ChecklistItemInput(checklistId, "여권 챙기기", null));

		verify(itemHandler).handle(new com.soomgil.planning.application.command.CreateChecklistItemCommand(
			tripId, checklistId, userId, "여권 챙기기", null
		));
	}

	@Test
	void factoryExposesOnlyTheMethodsAllowedForEachIntent() {
		AiTripToolsFactory factory = factory();
		AiGuideRequest request = request(UUID.randomUUID(), UUID.randomUUID());

		assertThat(toolNames(factory.create(request, AiIntent.READ_ITINERARY)))
			.containsExactly("getCurrentItinerary");
		assertThat(toolNames(factory.create(request, AiIntent.WRITE_NOTE)))
			.containsExactly("upsertNote");
		assertThat(toolNames(factory.create(request, AiIntent.ADD_PLACE_TO_ITINERARY)))
			.containsExactly("addPlaceToItinerary");
		assertThat(toolNames(factory.create(request, AiIntent.DELETE_ITINERARY_ITEM)))
			.containsExactly("deleteItineraryItem");
		assertThat(toolNames(factory.create(request, AiIntent.MOVE_ITINERARY_ITEM)))
			.containsExactly("moveItineraryItem");
		assertThat(toolNames(factory.create(request, AiIntent.GENERAL_CHAT))).isEmpty();
		assertThat(toolNames(factory.create(request, AiIntent.HELP))).isEmpty();
		assertThat(toolNames(factory.create(request, AiIntent.AMBIGUOUS))).isEmpty();
	}

	private Set<String> toolNames(java.util.List<AiExecutableTools> toolObjects) {
		return toolObjects.stream()
			.flatMap(tool -> Arrays.stream(tool.getClass().getMethods()))
			.filter(method -> method.isAnnotationPresent(Tool.class))
			.map(java.lang.reflect.Method::getName)
			.collect(Collectors.toSet());
	}

	private AiTripToolsFactory factory() {
		return new AiTripToolsFactory(
			mock(FindItineraryHandler.class), mock(PlaceSearchQueryHandler.class),
			mock(ListPlaceRecommendationsQueryHandler.class), mock(UpsertNoteCommandHandler.class),
			mock(UpsertChecklistCommandHandler.class), mock(CreateChecklistItemCommandHandler.class),
			mock(AiItineraryToolService.class), audit()
		);
	}

	private AiToolAuditService audit() {
		return mock(AiToolAuditService.class);
	}

	private AiGuideRequest request(UUID tripId, UUID userId) {
		return new AiGuideRequest(
			tripId, userId, UUID.randomUUID(), UUID.randomUUID(), null,
			java.util.List.of(), "질문", null, null, null
		);
	}
}
