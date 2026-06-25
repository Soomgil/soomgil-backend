package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
	void distinguishesRecommendationLookupFromAddingRecommendedPlaces() {
		assertThat(model.classify(request("갈만한 여행지 추천해줘", null)).intent())
			.isEqualTo(AiIntent.RECOMMEND_PLACES);
		assertThat(model.classify(request("추천 여행지 3개 알아서 일정에 넣어줘", null)).intent())
			.isEqualTo(AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY);
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
