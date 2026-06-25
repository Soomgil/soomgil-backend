package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.api.dto.PlaceRecommendation;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
		assertThat(toolNames(factory.create(request, AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY)))
			.containsExactly("addRecommendedPlacesToItinerary");
		assertThat(toolNames(factory.create(request, AiIntent.DELETE_ITINERARY_ITEM)))
			.containsExactly("deleteItineraryItem");
		assertThat(toolNames(factory.create(request, AiIntent.MOVE_ITINERARY_ITEM)))
			.containsExactly("moveItineraryItem");
		assertThat(toolNames(factory.create(request, AiIntent.GENERAL_CHAT))).isEmpty();
		assertThat(toolNames(factory.create(request, AiIntent.HELP))).isEmpty();
		assertThat(toolNames(factory.create(request, AiIntent.AMBIGUOUS))).isEmpty();
	}

	@Test
	void addsRecommendedPlacesThroughRecommendationQueryAndItineraryCommands() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		ListPlaceRecommendationsQueryHandler recommendationHandler = mock(ListPlaceRecommendationsQueryHandler.class);
		AiItineraryToolService itineraryToolService = mock(AiItineraryToolService.class);
		when(recommendationHandler.handle(new ListPlaceRecommendationsQuery(
			tripId, "126.8,37.4,127.2,37.7", null, null, RecommendationTab.BASIC, 0, 4
		))).thenReturn(new PagedPlaceRecommendation(
			List.of(recommendation("성심당", "1"), recommendation("한밭수목원", "2")),
			new PageMeta(0, 4, 2L, 1, List.of())
		));
		when(itineraryToolService.addPlace(any(), any(), any(Long.class), any()))
			.thenReturn(new ItineraryMutationResult(tripId, 8L, null, null, null, null, List.of()))
			.thenReturn(new ItineraryMutationResult(tripId, 9L, null, null, null, null, List.of()));
		AiAddRecommendedPlacesTools tools = new AiAddRecommendedPlacesTools(
			request(tripId, userId), audit(), recommendationHandler, itineraryToolService
		);

		Object result = tools.addRecommendedPlacesToItinerary(
			new AiAddRecommendedPlacesTools.AddRecommendedPlacesInput(
				7L, "126.8,37.4,127.2,37.7", null, null, "BASIC", 2, null, null
			)
		);

		assertThat(result).isInstanceOf(AiAddRecommendedPlacesTools.BulkAddRecommendedPlacesResult.class);
		assertThat(((AiAddRecommendedPlacesTools.BulkAddRecommendedPlacesResult) result).versionAfter()).isEqualTo(9L);
		verify(recommendationHandler).handle(new ListPlaceRecommendationsQuery(
			tripId, "126.8,37.4,127.2,37.7", null, null, RecommendationTab.BASIC, 0, 4
		));
		verify(itineraryToolService).addPlace(
			org.mockito.ArgumentMatchers.eq(tripId),
			org.mockito.ArgumentMatchers.eq(userId),
			org.mockito.ArgumentMatchers.eq(7L),
			org.mockito.ArgumentMatchers.argThat(input -> "성심당".equals(input.placeName()))
		);
		verify(itineraryToolService).addPlace(
			org.mockito.ArgumentMatchers.eq(tripId),
			org.mockito.ArgumentMatchers.eq(userId),
			org.mockito.ArgumentMatchers.eq(8L),
			org.mockito.ArgumentMatchers.argThat(input -> "한밭수목원".equals(input.placeName()))
		);
	}

	@Test
	void generatesChecklistItemsIntoTheMatchingDayChecklist() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID dayThreeId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UpsertChecklistCommandHandler checklistHandler = mock(UpsertChecklistCommandHandler.class);
		CreateChecklistItemCommandHandler itemHandler = mock(CreateChecklistItemCommandHandler.class);
		when(checklistHandler.handle(new UpsertChecklistCommand(
			tripId, userId, PlanningScopeType.DAY, dayThreeId, "3일차 체크리스트"
		))).thenReturn(new PlanningMutationResponse(
			tripId, null, null, false, false,
			null,
			new Checklist(checklistId, tripId, PlanningScopeType.DAY, dayThreeId, "3일차 체크리스트", List.of()),
			null,
			null
		));
		AiGenerateChecklistTools tools = new AiGenerateChecklistTools(
			request(tripId, userId), audit(), checklistHandler, itemHandler
		);

		tools.generateChecklistItemsByDay(new AiGenerateChecklistTools.GenerateItemsByDayInput(List.of(
			new AiGenerateChecklistTools.DayChecklistInput(
				null, dayThreeId, "3일차 체크리스트", List.of("롯데월드 예매 확인"), null
			)
		)));

		verify(checklistHandler).handle(new UpsertChecklistCommand(
			tripId, userId, PlanningScopeType.DAY, dayThreeId, "3일차 체크리스트"
		));
		verify(itemHandler).handle(new CreateChecklistItemCommand(
			tripId, checklistId, userId, "롯데월드 예매 확인", 0
		));
	}

	private PlaceRecommendation recommendation(String name, String externalPlaceId) {
		return new PlaceRecommendation(
			new PlaceSummary(
				PlaceProvider.KTO, externalPlaceId, name, "대전", 36.0, 127.0,
				URI.create("https://example.com/" + externalPlaceId + ".jpg"), "관광지", PlaceSourceStatus.AVAILABLE
			),
			List.of(), 0, 1, null, null, "그룹 취향과 잘 맞아요.", 80
		);
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
