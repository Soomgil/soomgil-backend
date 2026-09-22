package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.ai.api.dto.AiToolCall;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LocalFallbackAiGuideModelTest {

	private final AiTripToolsFactory toolsFactory = mock(AiTripToolsFactory.class);
	private final LocalFallbackAiGuideModel model = new LocalFallbackAiGuideModel(toolsFactory, new ObjectMapper());

	@Test
	void alwaysAnswersGreetingWithoutExternalProvider() {
		AiGuideRequest request = request("ㅎㅇ", null);

		AiIntentDecision decision = model.classify(request);
		AiGuideReply reply = model.replyWithoutTools(request, decision);

		assertThat(decision.intent()).isEqualTo(AiIntent.GENERAL_CHAT);
		assertThat(reply.content()).contains("안녕하세요");
		assertThat(reply.toolCalls()).isEmpty();
	}

	@Test
	void executesOnlyTheNoteToolForAnExplicitNoteRequest() {
		AiNoteTools noteTools = mock(AiNoteTools.class);
		AiToolCall call = mock(AiToolCall.class);
		when(toolsFactory.create(any(), org.mockito.ArgumentMatchers.eq(AiIntent.WRITE_NOTE)))
			.thenReturn(List.of(noteTools));
		when(noteTools.executedCalls()).thenReturn(List.of(call));
		AiGuideRequest request = request("전체 메모에 렌터카 예약 확인이라고 써줘", mock(AiTripContext.class));

		AiGuideReply reply = model.replyWithWriteTools(
			request, new AiIntentDecision(AiIntent.WRITE_NOTE, 1.0, "test", null)
		);

		ArgumentCaptor<AiNoteTools.ScopedTextInput> input = ArgumentCaptor.forClass(AiNoteTools.ScopedTextInput.class);
		verify(noteTools).upsertNote(input.capture());
		assertThat(input.getValue().scope()).isEqualTo("TRIP");
		assertThat(input.getValue().text()).isEqualTo("렌터카 예약 확인");
		assertThat(reply.toolCalls()).containsExactly(call);
	}

	@Test
	void descriptiveAddRequestWithoutPlaceNameFallsBackToRecommendedPlaces() {
		AiAddPlaceTools addTools = mock(AiAddPlaceTools.class);
		AiPlaceSearchTools searchTools = mock(AiPlaceSearchTools.class);
		AiAddRecommendedPlacesTools recommendedTools = mock(AiAddRecommendedPlacesTools.class);
		AiToolCall call = mock(AiToolCall.class);
		when(toolsFactory.create(any(), org.mockito.ArgumentMatchers.eq(AiIntent.ADD_PLACE_TO_ITINERARY)))
			.thenReturn(List.of(addTools, searchTools));
		when(toolsFactory.create(any(), org.mockito.ArgumentMatchers.eq(AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY)))
			.thenReturn(List.of(recommendedTools));
		when(recommendedTools.executedCalls()).thenReturn(List.of(call));
		AiGuideRequest request = request("4일차에 어울리는 장소 3곳으로 일정 초안 만들어서 추가해줘", mock(AiTripContext.class));

		AiGuideReply reply = model.replyWithWriteTools(
			request, new AiIntentDecision(AiIntent.ADD_PLACE_TO_ITINERARY, 1.0, "test", null)
		);

		ArgumentCaptor<AiAddRecommendedPlacesTools.AddRecommendedPlacesInput> input =
			ArgumentCaptor.forClass(AiAddRecommendedPlacesTools.AddRecommendedPlacesInput.class);
		verify(recommendedTools).addRecommendedPlacesToItinerary(input.capture());
		assertThat(input.getValue().limit()).isEqualTo(3);
		verify(searchTools, org.mockito.Mockito.never()).searchPlaces(any());
		assertThat(reply.toolCalls()).containsExactly(call);
	}

	@Test
	void distinguishesRecommendationLookupFromAddingRecommendedPlaces() {
		assertThat(model.classify(request("갈만한 여행지 추천해줘", null)).intent())
			.isEqualTo(AiIntent.RECOMMEND_PLACES);
		assertThat(model.classify(request("추천 여행지 3개 알아서 일정에 넣어줘", null)).intent())
			.isEqualTo(AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY);
	}

	@Test
	void classifiesNaturalHelpSummaryAndOptimizationQuestions() {
		assertThat(model.classify(request("무슨 기능을 할 수 있어?", null)).intent())
			.isEqualTo(AiIntent.HELP);
		assertThat(model.classify(request("현재 전체 여행 일정을 요약하고 분석해줘", null)).intent())
			.isEqualTo(AiIntent.SUMMARIZE_ITINERARY);
		assertThat(model.classify(request("가까운 장소끼리 묶어서 전체 동선을 최적화해줘", null)).intent())
			.isEqualTo(AiIntent.OPTIMIZE_ROUTE);
	}

	@Test
	void classifiesWheelchairRemovalAsConditionFiltering() {
		assertThat(model.classify(request("휠체어 이용 불가 장소를 일정에서 제거해줘", null)).intent())
			.isEqualTo(AiIntent.FILTER_PLACES_BY_CONDITION);
	}

	@Test
	void localFallbackUsesAccessibilityMetadataWhenFilteringPlaces() {
		AiFilterPlacesTools filterTools = mock(AiFilterPlacesTools.class);
		AiToolCall call = mock(AiToolCall.class);
		when(toolsFactory.create(any(), org.mockito.ArgumentMatchers.eq(AiIntent.FILTER_PLACES_BY_CONDITION)))
			.thenReturn(List.of(filterTools));
		when(filterTools.executedCalls()).thenReturn(List.of(call));
		UUID blockedItemId = UUID.randomUUID();
		UUID supportedItemId = UUID.randomUUID();
		AiTripContext context = contextWithItems(
			item(blockedItemId, "성산일출봉", new AiTripContext.AccessibilitySummary("UNKNOWN", List.of(), List.of("WHEELCHAIR"))),
			item(supportedItemId, "오설록 티뮤지엄", new AiTripContext.AccessibilitySummary("FREE", List.of("WHEELCHAIR"), List.of()))
		);

		AiGuideReply reply = model.replyWithWriteTools(
			request("휠체어 이용 불가 시설 삭제해줘", context),
			new AiIntentDecision(AiIntent.FILTER_PLACES_BY_CONDITION, 1.0, "test", null)
		);

		ArgumentCaptor<AiFilterPlacesTools.RemoveItemsInput> input =
			ArgumentCaptor.forClass(AiFilterPlacesTools.RemoveItemsInput.class);
		verify(filterTools).removeItineraryItemsByCondition(input.capture());
		assertThat(input.getValue().itemIds()).containsExactly(blockedItemId);
		assertThat(reply.toolCalls()).containsExactly(call);
	}

	@Test
	void overallChecklistRequestWritesTripLevelChecklistInsteadOfDayChecklists() {
		AiGenerateChecklistTools checklistTools = mock(AiGenerateChecklistTools.class);
		AiToolCall call = mock(AiToolCall.class);
		when(toolsFactory.create(any(), org.mockito.ArgumentMatchers.eq(AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY)))
			.thenReturn(List.of(checklistTools));
		when(checklistTools.executedCalls()).thenReturn(List.of(call));
		AiTripContext context = contextWithItems(
			item(UUID.randomUUID(), "제주해수욕장", null),
			item(UUID.randomUUID(), "제주박물관", null)
		);

		AiGuideReply reply = model.replyWithWriteTools(
			request("현재 여행 계획을 보고 준비물 체크리스트를 자동으로 만들어줘", context),
			new AiIntentDecision(AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY, 1.0, "test", null)
		);

		ArgumentCaptor<AiGenerateChecklistTools.GenerateItemsInput> input =
			ArgumentCaptor.forClass(AiGenerateChecklistTools.GenerateItemsInput.class);
		verify(checklistTools).generateChecklistItems(input.capture());
		verify(checklistTools, never()).generateChecklistItemsByDay(any());
		assertThat(input.getValue().scope()).isEqualTo("TRIP");
		assertThat(input.getValue().itineraryDayId()).isNull();
		assertThat(input.getValue().items()).contains("입장권 예약 여부와 운영시간 확인하기");
		assertThat(reply.toolCalls()).containsExactly(call);
	}

	@Test
	void dayTitleDoesNotRestrictUnscheduledPlacePlacementByRegion() {
		AiOptimizeRouteTools routeTools = mock(AiOptimizeRouteTools.class);
		AiToolCall call = mock(AiToolCall.class);
		when(toolsFactory.create(any(), org.mockito.ArgumentMatchers.eq(AiIntent.OPTIMIZE_ROUTE)))
			.thenReturn(List.of(routeTools));
		when(routeTools.executedCalls()).thenReturn(List.of(call));
		UUID firstDayId = UUID.randomUUID();
		UUID secondDayId = UUID.randomUUID();
		UUID placeId = UUID.randomUUID();
		AiTripContext.ItemSummary place = new AiTripContext.ItemSummary(
			placeId, 0, "PLACE", "KTO", "daejeon-place", "한밭수목원", "대전광역시", 36.35, 127.39, null
		);
		AiTripContext.ItemSummary firstDayPlace = new AiTripContext.ItemSummary(
			UUID.randomUUID(), 0, "PLACE", "KTO", "first-day-place", "성심당", "대전광역시", 36.33, 127.43, null
		);
		AiTripContext context = new AiTripContext(
			new AiTripContext.TripSummary(UUID.randomUUID(), "대전 여행", "대전", "PLANNING", "OWNER", 1L),
			List.of(),
			List.of(
				new AiTripContext.DaySummary(firstDayId, "DAY", 1, null, "1일차", List.of(firstDayPlace)),
				new AiTripContext.DaySummary(secondDayId, "DAY", 2, null, "서울 동부 코스", List.of()),
				new AiTripContext.DaySummary(UUID.randomUUID(), "UNSCHEDULED", null, null, "일차 미정", List.of(place))
			),
			List.of(), List.of(), List.of(), List.of()
		);
		AiGuideRequest request = request("일차 미정 관광지를 일차에 배치해줘", context);
		AiIntentDecision decision = model.classify(request);

		AiGuideReply reply = model.replyWithWriteTools(request, decision);

		assertThat(decision.intent()).isEqualTo(AiIntent.OPTIMIZE_ROUTE);
		ArgumentCaptor<AiOptimizeRouteTools.OptimizeRouteInput> input =
			ArgumentCaptor.forClass(AiOptimizeRouteTools.OptimizeRouteInput.class);
		verify(routeTools).optimizeRoute(input.capture());
		assertThat(input.getValue().moves()).hasSize(1);
		assertThat(input.getValue().moves().getFirst().itemId()).isEqualTo(placeId);
		assertThat(input.getValue().moves().getFirst().itineraryDayId()).isEqualTo(secondDayId);
		assertThat(reply.toolCalls()).containsExactly(call);
	}

	private AiGuideRequest request(String question, AiTripContext context) {
		return new AiGuideRequest(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
			List.of(), question, 1L, null, context
		);
	}

	private AiTripContext contextWithItems(AiTripContext.ItemSummary... items) {
		return new AiTripContext(
			new AiTripContext.TripSummary(UUID.randomUUID(), "제주 여행", "제주", "PLANNING", "OWNER", 1L),
			List.of(),
			List.of(new AiTripContext.DaySummary(UUID.randomUUID(), "DAY", 1, null, "1일차", List.of(items))),
			List.of(),
			List.of(),
			List.of(),
			List.of()
		);
	}

	private AiTripContext.ItemSummary item(
		UUID id,
		String placeName,
		AiTripContext.AccessibilitySummary accessibility
	) {
		return new AiTripContext.ItemSummary(
			id, 0, "PLACE", "KTO", "seed-" + id, placeName, "제주특별자치도", 33.45, 126.55, accessibility
		);
	}
}
