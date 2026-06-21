package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class AiChatServiceTest {

	private final TripAccessGuard accessGuard = mock(TripAccessGuard.class);
	private final AiChatMapper mapper = mock(AiChatMapper.class);
	private final AiGuideModel model = mock(AiGuideModel.class);
	private final FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final AiChatService service = new AiChatService(
		accessGuard, mapper, model, displayNameHandler, messagingTemplate
	);

	@Test
	void storesTheUserQuestionAndAssistantAnswerInTheSharedTripSession() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		AiChatSessionRow session = new AiChatSessionRow(sessionId, tripId, "ACTIVE", null, null, Instant.now());
		when(mapper.findSessionByTripId(tripId)).thenReturn(session);
		when(mapper.findRecentMessages(sessionId, 20)).thenReturn(List.of());
		when(model.reply(any())).thenReturn("비 오는 날에는 박물관을 추천해요.");
		when(mapper.findMessageById(any())).thenAnswer(invocation -> new AiChatMessageRow(
			invocation.getArgument(0), sessionId, null, "ASSISTANT",
			"비 오는 날에는 박물관을 추천해요.", null, Instant.now(), null, null
		));

		var response = service.createMessage(tripId, userId, "비 오는 날 갈 곳 알려줘", null);

		assertThat(response.message().content()).contains("박물관");
		verify(accessGuard).requireActiveMember(tripId, userId);
		verify(mapper).insertMessage(any(), org.mockito.ArgumentMatchers.eq(sessionId),
			org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq("USER"),
			org.mockito.ArgumentMatchers.eq("비 오는 날 갈 곳 알려줘"), any());
		verify(mapper).insertMessage(any(), org.mockito.ArgumentMatchers.eq(sessionId),
			org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("ASSISTANT"),
			org.mockito.ArgumentMatchers.contains("박물관"), any());
		verify(messagingTemplate).convertAndSend(
			org.mockito.ArgumentMatchers.eq("/topic/trips/" + tripId + "/ai"), any(Object.class)
		);
	}
}
