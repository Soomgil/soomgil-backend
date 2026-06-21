package com.soomgil.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.notification.infrastructure.persistence.NotificationRow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

	@Test
	void listsOnlyTheCurrentUsersUnreadNotifications() {
		NotificationMapper mapper = mock(NotificationMapper.class);
		UUID userId = UUID.randomUUID();
		when(mapper.countByRecipient(userId, true)).thenReturn(1L);
		when(mapper.findByRecipient(userId, true, 0, 20)).thenReturn(List.of(new NotificationRow(
			UUID.randomUUID(), null, null, null, "TRIP_INVITE", "초대", null,
			"{\"tripId\":\"00000000-0000-0000-0000-000000000001\","
				+ "\"inviteId\":\"00000000-0000-0000-0000-000000000002\",\"inviteCode\":\"ABC\"}",
			null, Instant.now()
		)));
		NotificationService service = new NotificationService(mapper, new ObjectMapper().findAndRegisterModules());

		var result = service.list(userId, true, 0, 20);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().payload().inviteCode()).isEqualTo("ABC");
		verify(mapper).findByRecipient(userId, true, 0, 20);
	}
}
