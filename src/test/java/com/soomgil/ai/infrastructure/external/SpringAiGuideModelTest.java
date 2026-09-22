package com.soomgil.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.ai.application.AiChecklistRequestScope;
import com.soomgil.ai.application.AiGenerateChecklistTools;
import com.soomgil.ai.application.AiGuideRequest;
import com.soomgil.ai.application.AiIntent;
import com.soomgil.ai.application.AiIntentDecision;
import com.soomgil.ai.application.AiTripContext;
import com.soomgil.ai.application.AiTripToolsFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

class SpringAiGuideModelTest {

	@Test
	void tripWideChecklistRoutingExcludesSingleItemAndDaySpecificRequests() {
		assertThat(AiChecklistRequestScope.isTripWide("공동 체크리스트에 보조배터리 항목을 추가해줘"))
			.isFalse();
		assertThat(AiChecklistRequestScope.isTripWide("여행 계획을 보고 일차별 준비물 체크리스트를 만들어줘"))
			.isFalse();
	}

	@Test
	void overallTravelChecklistUsesTripScopeAndCreatesPreparationItems() {
		ChatModel chatModel = mock(ChatModel.class);
		AiTripToolsFactory toolsFactory = mock(AiTripToolsFactory.class);
		AiGenerateChecklistTools checklistTools = mock(AiGenerateChecklistTools.class);
		AiToolCall call = mock(AiToolCall.class);
		when(toolsFactory.create(any(), eq(AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY)))
			.thenReturn(List.of(checklistTools));
		when(checklistTools.executedCalls()).thenReturn(List.of(call));
		SpringAiGuideModel model = new SpringAiGuideModel(chatModel, toolsFactory, new ObjectMapper());
		UUID tripId = UUID.randomUUID();
		UUID dayId = UUID.randomUUID();
		AiTripContext context = new AiTripContext(
			new AiTripContext.TripSummary(tripId, "제주 여행", "제주", "PLANNING", "OWNER", 1L),
			List.of(),
			List.of(new AiTripContext.DaySummary(dayId, "DAY", 1, null, "해변 코스", List.of(
				new AiTripContext.ItemSummary(UUID.randomUUID(), 0, "PLACE", "KTO", "beach-1",
					"제주해수욕장", "제주", 33.0, 126.0, null)
			))),
			List.of(), List.of(), List.of(), List.of()
		);
		AiGuideRequest request = new AiGuideRequest(
			tripId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, List.of(),
			"현재 여행 계획을 보고 준비물 체크리스트를 자동으로 만들어줘", null, null, context
		);

		var reply = model.replyWithWriteTools(request,
			new AiIntentDecision(AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY, 1.0, "test", null));

		ArgumentCaptor<AiGenerateChecklistTools.GenerateItemsInput> input =
			ArgumentCaptor.forClass(AiGenerateChecklistTools.GenerateItemsInput.class);
		verify(checklistTools).generateChecklistItems(input.capture());
		assertThat(input.getValue().scope()).isEqualTo("TRIP");
		assertThat(input.getValue().itineraryDayId()).isNull();
		assertThat(input.getValue().items()).contains("야외 일정용 물과 자외선 차단제 챙기기");
		assertThat(reply.toolCalls()).containsExactly(call);
		verify(chatModel, never()).call(any(Prompt.class));
	}
}
