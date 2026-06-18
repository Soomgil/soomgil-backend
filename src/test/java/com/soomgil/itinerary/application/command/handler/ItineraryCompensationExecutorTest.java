package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ItineraryCompensationExecutorTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ITEM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-06-18T00:00:00Z");

	private final ItineraryCommandRepository repository = mock(ItineraryCommandRepository.class);
	private final ItineraryCompensationExecutor executor = new ItineraryCompensationExecutor(repository, new ObjectMapper());

	@Test
	void softDeletesCreatedItemForUndo() {
		String payload = "{\"action\":\"DELETE_ITINERARY_ITEM\",\"itemId\":\"" + ITEM_ID + "\"}";
		when(repository.softDeleteItem(TRIP_ID, ITEM_ID, USER_ID, NOW)).thenReturn(true);

		assertThat(executor.supports(payload)).isTrue();
		executor.execute(TRIP_ID, USER_ID, payload, NOW);

		verify(repository).softDeleteItem(TRIP_ID, ITEM_ID, USER_ID, NOW);
	}

	@Test
	void rejectsChangedCompensationTarget() {
		String payload = "{\"action\":\"DELETE_ITINERARY_ITEM\",\"itemId\":\"" + ITEM_ID + "\"}";

		assertThatThrownBy(() -> executor.execute(TRIP_ID, USER_ID, payload, NOW))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT));
	}
}
