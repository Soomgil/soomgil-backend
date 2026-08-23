package com.soomgil.collaboration.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryMapObjectLeaseStoreTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID DRAWING_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

	private final InMemoryMapObjectLeaseStore store = new InMemoryMapObjectLeaseStore();

	@Test
	void grantsRenewsAndReleasesOwnedLease() {
		var acquired = store.acquire(TRIP_ID, DRAWING_ID, USER_ID, "session-1", NOW);
		var renewed = store.renew(TRIP_ID, DRAWING_ID, USER_ID, "session-1", NOW.plusSeconds(5));

		assertThat(acquired.expiresAt()).isEqualTo(NOW.plusSeconds(15));
		assertThat(renewed.expiresAt()).isEqualTo(NOW.plusSeconds(20));
		store.requireOwned(TRIP_ID, DRAWING_ID, USER_ID, "session-1", NOW.plusSeconds(6));
		assertThat(store.release(TRIP_ID, DRAWING_ID, USER_ID, "session-1")).isTrue();
		assertThat(store.find(TRIP_ID, DRAWING_ID, NOW.plusSeconds(6))).isEmpty();
	}

	@Test
	void rejectsCompetingEditorUntilLeaseExpires() {
		store.acquire(TRIP_ID, DRAWING_ID, USER_ID, "session-1", NOW);

		assertThatThrownBy(() -> store.acquire(
			TRIP_ID, DRAWING_ID, OTHER_USER_ID, "session-2", NOW.plusSeconds(14)
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT));

		var acquired = store.acquire(
			TRIP_ID, DRAWING_ID, OTHER_USER_ID, "session-2", NOW.plusSeconds(15)
		);
		assertThat(acquired.userId()).isEqualTo(OTHER_USER_ID);
	}

	@Test
	void releasesEveryLeaseOwnedByDisconnectedSession() {
		store.acquire(TRIP_ID, DRAWING_ID, USER_ID, "session-1", NOW);
		store.acquire(TRIP_ID, UUID.randomUUID(), USER_ID, "session-1", NOW);

		assertThat(store.releaseSession("session-1")).hasSize(2);
	}

	@Test
	void doesNotReportReleaseWhenLeaseIsMissingOrOwnedByAnotherSession() {
		assertThat(store.release(TRIP_ID, DRAWING_ID, USER_ID, "session-1")).isFalse();
		store.acquire(TRIP_ID, DRAWING_ID, USER_ID, "session-1", NOW);

		assertThat(store.release(TRIP_ID, DRAWING_ID, USER_ID, "session-2")).isFalse();
		assertThat(store.find(TRIP_ID, DRAWING_ID, NOW)).isPresent();
	}
}
