package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.ai.infrastructure.persistence.AiChatMapper;
import com.soomgil.ai.infrastructure.persistence.AiChatMessageRow;
import com.soomgil.ai.infrastructure.persistence.AiChatSessionRow;
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class AiChatServiceTest {

	private final TripAccessGuard accessGuard = mock(TripAccessGuard.class);
	private final AiChatMapper mapper = mock(AiChatMapper.class);
	private final AiGuideModel model = mock(AiGuideModel.class);
	private final AiTripContextService contextService = mock(AiTripContextService.class);
	private final FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final AiChatService service = new AiChatService(
		accessGuard, mapper, model, contextService, displayNameHandler, messagingTemplate
	);
	private UUID tripId;
	private UUID userId;
	private UUID sessionId;
	private AiTripContext tripContext;

	@BeforeEach
	void setUp() {
		tripId = UUID.randomUUID();
		userId = UUID.randomUUID();
		sessionId = UUID.randomUUID();
		tripContext = mock(AiTripContext.class);
		when(mapper.findSessionByTripId(tripId)).thenReturn(
			new AiChatSessionRow(sessionId, tripId, "ACTIVE", null, null, Instant.now())
		);
		when(mapper.findRecentMessages(sessionId, 20)).thenReturn(List.of());
		when(contextService.load(tripId, userId)).thenReturn(tripContext);
	}

	@Test
	void storesMessagesAndKeepsTheExistingWebsocketResponseFlow() {
		stubAssistant("비 오는 날에는 박물관을 추천해요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.RECOMMEND_PLACES));
		when(model.replyWithReadTools(any(), any())).thenReturn(
			new AiGuideReply("비 오는 날에는 박물관을 추천해요.", List.of())
		);

		var response = service.createMessage(tripId, userId, "비 오는 날 갈 곳 추천해줘", null);

		assertThat(response.message().content()).contains("박물관");
		verify(accessGuard).requireActiveMember(tripId, userId);
		verify(mapper).insertMessage(any(), eq(sessionId), eq(userId), eq("USER"),
			eq("비 오는 날 갈 곳 추천해줘"), any());
		verify(mapper).insertMessage(any(), eq(sessionId), org.mockito.ArgumentMatchers.isNull(),
			eq("ASSISTANT"), org.mockito.ArgumentMatchers.contains("박물관"), any());
		verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/trips/" + tripId + "/ai"), any(Object.class));
		ArgumentCaptor<AiGuideRequest> requestCaptor = ArgumentCaptor.forClass(AiGuideRequest.class);
		verify(model).classify(requestCaptor.capture());
		assertThat(requestCaptor.getValue().tripContext()).isNull();
		ArgumentCaptor<AiGuideRequest> replyCaptor = ArgumentCaptor.forClass(AiGuideRequest.class);
		verify(model).replyWithReadTools(replyCaptor.capture(), any());
		assertThat(replyCaptor.getValue().tripContext()).isSameAs(tripContext);
	}

	@Test
	void greetingNeverExposesTools() {
		stubAssistant("안녕하세요! 여행 계획을 함께 정리해드릴게요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.GENERAL_CHAT));
		when(model.replyWithoutTools(any(), any())).thenReturn(
			new AiGuideReply("안녕하세요! 여행 계획을 함께 정리해드릴게요.", List.of())
		);

		var response = service.createMessage(tripId, userId, "안녕", null);

		assertThat(response.toolCalls()).isEmpty();
		verify(model).replyWithoutTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.GENERAL_CHAT
		));
		verify(model, never()).replyWithReadTools(any(), any());
		verify(model, never()).replyWithWriteTools(any(), any());
		verify(contextService, never()).load(any(), any());
	}

	@Test
	void shorthandGreetingNeverLoadsTripContextOrExposesTools() {
		stubAssistant("안녕하세요!");
		when(model.classify(any())).thenReturn(decision(AiIntent.GENERAL_CHAT));
		when(model.replyWithoutTools(any(), any())).thenReturn(new AiGuideReply("안녕하세요!", List.of()));

		var response = service.createMessage(tripId, userId, "ㅎㅇ", null);

		assertThat(response.toolCalls()).isEmpty();
		verify(model).replyWithoutTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.GENERAL_CHAT
		));
		verify(model, never()).replyWithReadTools(any(), any());
		verify(model, never()).replyWithWriteTools(any(), any());
		verify(contextService, never()).load(any(), any());
	}

	@Test
	void helpQuestionNeverExposesTools() {
		stubAssistant("일정 조회와 장소 추천 등을 도와드릴 수 있어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.HELP));
		when(model.replyWithoutTools(any(), any())).thenReturn(
			new AiGuideReply("일정 조회와 장소 추천 등을 도와드릴 수 있어요.", List.of())
		);

		var response = service.createMessage(tripId, userId, "뭐 할 수 있어?", null);

		assertThat(response.toolCalls()).isEmpty();
		verify(model).replyWithoutTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.HELP
		));
		verify(model, never()).replyWithReadTools(any(), any());
		verify(model, never()).replyWithWriteTools(any(), any());
		verify(contextService, never()).load(any(), any());
	}

	@Test
	void itineraryReadUsesOnlyTheReadStage() {
		stubAssistant("현재 일정은 2일차까지 있어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.READ_ITINERARY));
		when(model.replyWithReadTools(any(), any())).thenReturn(
			new AiGuideReply("현재 일정은 2일차까지 있어요.", List.of())
		);

		service.createMessage(tripId, userId, "현재 일정 보여줘", null);

		verify(model).replyWithReadTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.READ_ITINERARY
		));
		verify(model, never()).replyWithWriteTools(any(), any());
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void noteWriteUsesOnlyTheWriteStage() {
		stubAssistant("공동 메모에 저장했어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_NOTE));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("공동 메모에 저장했어요.", List.of())
		);

		service.createMessage(tripId, userId, "전체 메모에 렌터카 예약 확인이라고 써줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.WRITE_NOTE
		));
		verify(model, never()).replyWithReadTools(any(), any());
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void checklistGenerationCueUsesTheGenerationIntentEvenIfClassifierIsGenericWrite() {
		stubAssistant("일정 기준으로 체크리스트를 만들었어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_CHECKLIST));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("일정 기준으로 체크리스트를 만들었어요.", List.of())
		);

		service.createMessage(tripId, userId, "이번 여행 체크리스트 만들어줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY
		));
		verify(model, never()).replyWithReadTools(any(), any());
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void travelPrepChecklistCueDoesNotWriteTheLiteralTextAfterChecklistMarker() {
		stubAssistant("여행 계획 기준으로 준비물 체크리스트를 만들었어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_CHECKLIST));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("여행 계획 기준으로 준비물 체크리스트를 만들었어요.", List.of())
		);

		service.createMessage(tripId, userId, "체크리스트에 여행전 준비물 추가", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY
		));
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void shortTravelChecklistCueStillUsesGenerationIntent() {
		stubAssistant("여행 계획 기준으로 준비물 체크리스트를 만들었어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_CHECKLIST));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("여행 계획 기준으로 준비물 체크리스트를 만들었어요.", List.of())
		);

		service.createMessage(tripId, userId, "체크리스트에 여행", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY
		));
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void deleteCueUsesTheDeleteToolEvenIfClassifierIsAmbiguous() {
		stubAssistant("경복궁을 일정에서 삭제했어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.AMBIGUOUS));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("경복궁을 일정에서 삭제했어요.", List.of())
		);

		service.createMessage(tripId, userId, "경복궁 지워줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.DELETE_ITINERARY_ITEM
		));
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void numberedPlaceAddCueUsesRecommendedPlaceAddEvenIfClassifierIsGenericAdd() {
		stubAssistant("추천 여행지 2개를 2일차에 추가했어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.ADD_PLACE_TO_ITINERARY));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("추천 여행지 2개를 2일차에 추가했어요.", List.of())
		);

		service.createMessage(tripId, userId, "2일차에 여행지 2개 추가해줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY
		));
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void askingWhatIsInTheChecklistReadsInsteadOfGeneratingNewItems() {
		stubAssistant("체크리스트에 3개 항목이 있어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY));
		when(model.replyWithReadTools(any(), any())).thenReturn(
			new AiGuideReply("체크리스트에 3개 항목이 있어요.", List.of())
		);

		service.createMessage(tripId, userId, "체크리스트에 뭐 있는지 알려줘", null);

		verify(model).replyWithReadTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.READ_PLANNING
		));
		verify(model, never()).replyWithWriteTools(any(), any());
	}

	@Test
	void askingToBuildAChecklistStillGenerates() {
		stubAssistant("일정 기준으로 체크리스트를 만들었어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_CHECKLIST));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("일정 기준으로 체크리스트를 만들었어요.", List.of())
		);

		service.createMessage(tripId, userId, "체크리스트 자동으로 만들어줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY
		));
	}

	@Test
	void addingADayUsesDayManagementInsteadOfPlaceAdd() {
		stubAssistant("5일차를 추가했어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.ADD_PLACE_TO_ITINERARY));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("5일차를 추가했어요.", List.of())
		);

		service.createMessage(tripId, userId, "5일차 하나 추가해줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.MANAGE_ITINERARY_DAY
		));
	}

	@Test
	void transportModeRouteRequestUsesTheRouteConnectionIntent() {
		stubAssistant("2일차 장소들을 자전거 경로로 연결했어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.OPTIMIZE_ROUTE));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("2일차 장소들을 자전거 경로로 연결했어요.", List.of())
		);

		service.createMessage(tripId, userId, "2일차 자전거로 경로 이어줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.CONNECT_DAY_ROUTES
		));
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void naturallyPhrasedRouteRequestIsNoLongerRejectedByCueMatching() {
		stubAssistant("도보 경로로 연결했어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.CONNECT_DAY_ROUTES));
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("도보 경로로 연결했어요.", List.of())
		);

		service.createMessage(tripId, userId, "여기 걸어서 가는 길로 바꿔줄래", null);

		verify(model).replyWithWriteTools(any(), any());
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void readIntentRunsEvenWhenTheQuestionUsesUnexpectedWording() {
		stubAssistant("체크리스트에는 3개 항목이 있어요.");
		when(model.classify(any())).thenReturn(decision(AiIntent.READ_PLANNING));
		when(model.replyWithReadTools(any(), any())).thenReturn(
			new AiGuideReply("체크리스트에는 3개 항목이 있어요.", List.of())
		);

		service.createMessage(tripId, userId, "우리 준비물 어디까지 했더라", null);

		verify(model).replyWithReadTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.READ_PLANNING
		));
		verify(model, never()).replyWithoutTools(any(), any());
	}

	@Test
	void lowConfidenceDeletionWithoutAnyCueStillAsksBack() {
		stubAssistant("어떤 장소를 삭제할까요?");
		when(model.classify(any())).thenReturn(
			new AiIntentDecision(AiIntent.DELETE_ITINERARY_ITEM, 0.4, "test", null)
		);
		when(model.replyWithoutTools(any(), any())).thenReturn(
			new AiGuideReply("어떤 장소를 삭제할까요?", List.of())
		);

		service.createMessage(tripId, userId, "여기 좀 정리해줘", null);

		verify(model).replyWithoutTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.AMBIGUOUS
		));
		verify(model, never()).replyWithWriteTools(any(), any());
	}

	@Test
	void reversibleWriteRunsOnLowConfidenceWhenTheWordingIsExplicit() {
		stubAssistant("경복궁을 3일차로 옮겼어요.");
		when(model.classify(any())).thenReturn(
			new AiIntentDecision(AiIntent.MOVE_ITINERARY_ITEM, 0.3, "test", null)
		);
		when(model.replyWithWriteTools(any(), any())).thenReturn(
			new AiGuideReply("경복궁을 3일차로 옮겼어요.", List.of())
		);

		service.createMessage(tripId, userId, "경복궁 3일차로 옮겨줘", null);

		verify(model).replyWithWriteTools(any(), org.mockito.ArgumentMatchers.argThat(
			decision -> decision.intent() == AiIntent.MOVE_ITINERARY_ITEM
		));
	}

	private AiIntentDecision decision(AiIntent intent) {
		return new AiIntentDecision(intent, 0.99, "test", null);
	}

	private void stubAssistant(String content) {
		when(mapper.findMessageById(any())).thenAnswer(invocation -> new AiChatMessageRow(
			invocation.getArgument(0), sessionId, null, "ASSISTANT", content, null,
			Instant.now(), null, null
		));
	}
}
