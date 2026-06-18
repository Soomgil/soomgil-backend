package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.application.command.RequestAccountDeletionCommand;
import com.soomgil.user.application.service.ActiveOwnerTripChecker;
import com.soomgil.user.infrastructure.persistence.UserAccountCommandMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link RequestAccountDeletionCommandHandler} 단위 테스트.
 */
class RequestAccountDeletionCommandHandlerTest {

	private final UserAccountCommandMapper mapper = mock(UserAccountCommandMapper.class);
	private final ActiveOwnerTripChecker tripChecker = mock(ActiveOwnerTripChecker.class);
	private final RequestAccountDeletionCommandHandler handler =
		new RequestAccountDeletionCommandHandler(mapper, tripChecker);

	@Test
	@DisplayName("활성 OWNER 여행방이 없으면 삭제 예약을 기록하고 NoResult를 반환한다")
	void recordsDeletionRequestWhenNoActiveOwnerTrip() {
		UUID userId = UUID.randomUUID();
		when(tripChecker.hasActiveOwnerTrip(userId)).thenReturn(false);
		when(mapper.markPendingDeletion(eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
			.thenReturn(1);

		NoResult result = handler.handle(new RequestAccountDeletionCommand(userId));

		assertThat(result).isEqualTo(NoResult.INSTANCE);
		verify(mapper, times(1)).markPendingDeletion(eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class));
	}

	@Test
	@DisplayName("삭제 예정 시각은 요청 시각으로부터 30일 후다")
	void schedulesDeletion30DaysAhead() {
		UUID userId = UUID.randomUUID();
		when(tripChecker.hasActiveOwnerTrip(userId)).thenReturn(false);
		when(mapper.markPendingDeletion(any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
			.thenReturn(1);

		OffsetDateTime before = OffsetDateTime.now();
		handler.handle(new RequestAccountDeletionCommand(userId));
		OffsetDateTime after = OffsetDateTime.now();

		verify(mapper, times(1)).markPendingDeletion(
			eq(userId),
			any(OffsetDateTime.class),
			org.mockito.ArgumentMatchers.argThat(scheduledAt ->
				scheduledAt.isAfter(before.plus(Duration.ofDays(30)).minusMinutes(1))
				&& scheduledAt.isBefore(after.plus(Duration.ofDays(30)).plusMinutes(1))
			)
		);
	}

	@Test
	@DisplayName("활성 OWNER 여행방이 있으면 409 ACCOUNT_DELETION_BLOCKED_BY_ACTIVE_OWNER_TRIP 예외를 던진다")
	void blocksWhenActiveOwnerTripExists() {
		UUID userId = UUID.randomUUID();
		when(tripChecker.hasActiveOwnerTrip(userId)).thenReturn(true);

		assertThatThrownBy(() -> handler.handle(new RequestAccountDeletionCommand(userId)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.ACCOUNT_DELETION_BLOCKED_BY_ACTIVE_OWNER_TRIP);

		verify(mapper, never()).markPendingDeletion(any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class));
	}
}
