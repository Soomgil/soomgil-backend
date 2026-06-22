package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
		verify(messagingTemplate).convertAndSend(eq("/topic/trips/" + tripId + "/ai"), any(Object.class));
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
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_NOTE));
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
		when(model.classify(any())).thenReturn(decision(AiIntent.WRITE_NOTE));
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
		when(model.classify(any())).thenReturn(decision(AiIntent.ADD_PLACE_TO_ITINERARY));
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
