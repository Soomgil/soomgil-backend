package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.common.cqrs.NoResult;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.user.application.command.RequestAccountDeletionCommand;
import com.soomgil.user.infrastructure.persistence.UserAccountCommandMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link RequestAccountDeletionCommandHandler} 단위 테스트. */
class RequestAccountDeletionCommandHandlerTest {

	private final UserAccountCommandMapper mapper = mock(UserAccountCommandMapper.class);
	private final TripCommandRepository tripCommandRepository = mock(TripCommandRepository.class);
	private final RequestAccountDeletionCommandHandler handler =
		new RequestAccountDeletionCommandHandler(mapper, tripCommandRepository);

	@Test
	@DisplayName("여행방 관계를 정리한 뒤 즉시 탈퇴하고 개인정보를 정리한다")
	void deletesAccountImmediatelyAfterDepartingTrips() {
		UUID userId = UUID.randomUUID();
		when(mapper.markDeleted(eq(userId), any(OffsetDateTime.class))).thenReturn(1);

		NoResult result = handler.handle(new RequestAccountDeletionCommand(userId));

		assertThat(result).isEqualTo(NoResult.INSTANCE);
		verify(mapper).markDeleted(eq(userId), any(OffsetDateTime.class));
		verify(tripCommandRepository).departUserForAccountDeletion(eq(userId), any(Instant.class));
		verify(mapper).revokeSessions(eq(userId), any(OffsetDateTime.class));
		verify(mapper).deleteEmailVerificationTokens(userId);
		verify(mapper).deletePasswordResetTokens(userId);
		verify(mapper).deletePasswordCredential(userId);
		verify(mapper).deleteOAuthIdentities(userId);
		verify(mapper).anonymizeEmailAddresses(eq(userId), any(OffsetDateTime.class));
		verify(mapper).anonymizeProfile(userId);
		verify(mapper).disableEmailSettings(eq(userId), any(OffsetDateTime.class));
		verify(mapper).anonymizeSecurityEvents(userId);
	}

	@Test
	@DisplayName("이미 탈퇴 처리된 계정은 멱등하게 성공하고 정리를 반복하지 않는다")
	void alreadyDeletedAccountIsIdempotent() {
		UUID userId = UUID.randomUUID();
		when(mapper.markDeleted(eq(userId), any(OffsetDateTime.class))).thenReturn(0);

		NoResult result = handler.handle(new RequestAccountDeletionCommand(userId));

		assertThat(result).isEqualTo(NoResult.INSTANCE);
		verify(mapper).markDeleted(eq(userId), any(OffsetDateTime.class));
		verify(tripCommandRepository, never()).departUserForAccountDeletion(any(UUID.class), any(Instant.class));
		verify(mapper, never()).revokeSessions(any(UUID.class), any(OffsetDateTime.class));
		verify(mapper, never()).anonymizeEmailAddresses(any(UUID.class), any(OffsetDateTime.class));
	}
}
