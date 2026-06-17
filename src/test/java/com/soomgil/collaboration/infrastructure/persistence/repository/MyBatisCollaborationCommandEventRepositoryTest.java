package com.soomgil.collaboration.infrastructure.persistence.repository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationEventBroadcaster;
import com.soomgil.collaboration.infrastructure.persistence.mapper.CollaborationCommandEventMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyBatisCollaborationCommandEventRepositoryTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	private final CollaborationCommandEventMapper mapper = mock(CollaborationCommandEventMapper.class);
	private final CollaborationEventBroadcaster broadcaster = mock(CollaborationEventBroadcaster.class);
	private final MyBatisCollaborationCommandEventRepository repository =
		new MyBatisCollaborationCommandEventRepository(mapper, broadcaster);

	@Test
	void savesReturningIdAndBroadcasts() {
		CollaborationCommandEvent event = event();
		when(mapper.insertEventReturningId(event)).thenReturn(15L);

		org.assertj.core.api.Assertions.assertThat(repository.saveReturningId(event)).isEqualTo(15L);

		verify(broadcaster).broadcast(15L, event);
	}

	@Test
	void saveAlsoBroadcastsPersistedEvent() {
		CollaborationCommandEvent event = event();
		when(mapper.insertEventReturningId(event)).thenReturn(16L);

		repository.save(event);

		verify(broadcaster).broadcast(16L, event);
	}

	private CollaborationCommandEvent event() {
		return new CollaborationCommandEvent(
			TRIP_ID,
			USER_ID,
			"session-1",
			"USER",
			"UPDATE_ITINERARY_ITEM",
			"ITINERARY_ITEM",
			UUID.fromString("30000000-0000-0000-0000-000000000001"),
			1L,
			2L,
			"{\"id\":\"30000000-0000-0000-0000-000000000001\"}",
			null,
			null,
			Instant.parse("2026-06-18T00:00:00Z")
		);
	}
}
