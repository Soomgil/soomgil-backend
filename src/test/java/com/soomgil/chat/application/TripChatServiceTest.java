package com.soomgil.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.chat.infrastructure.persistence.TripChatMessageMapper;
import com.soomgil.chat.infrastructure.persistence.TripChatMessageRow;
import com.soomgil.global.error.BusinessException;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class TripChatServiceTest {

	private final TripAccessGuard accessGuard = mock(TripAccessGuard.class);
	private final TripChatMessageMapper mapper = mock(TripChatMessageMapper.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final TripChatService service = new TripChatService(accessGuard, mapper, messagingTemplate);

	@Test
	void listsNewestMessagesAndReturnsADeletedTombstone() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(mapper.findByTrip(tripId, 0, 3)).thenReturn(List.of(
			row(tripId, userId, null), row(tripId, userId, Instant.now()), row(tripId, userId, null)
		));

		var result = service.list(tripId, userId, 0, 2);

		assertThat(result.items()).hasSize(2);
		assertThat(result.items().get(1).content()).isNull();
		assertThat(result.page().hasMore()).isTrue();
		verify(accessGuard).requireActiveMember(tripId, userId);
	}

	@Test
	void preventsDeletingAnotherUsersMessage() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		when(mapper.findById(messageId)).thenReturn(row(tripId, UUID.randomUUID(), null));

		assertThatThrownBy(() -> service.delete(tripId, messageId, userId))
			.isInstanceOf(BusinessException.class);
	}

	private TripChatMessageRow row(UUID tripId, UUID senderId, Instant deletedAt) {
		return new TripChatMessageRow(
			UUID.randomUUID(), tripId, senderId, "사용자", null, "안녕하세요", deletedAt, Instant.now()
		);
	}
}
