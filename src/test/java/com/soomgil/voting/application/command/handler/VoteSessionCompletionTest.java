package com.soomgil.voting.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledCommand;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledResult;
import com.soomgil.itinerary.application.command.dto.AddedUnscheduledPlace;
import com.soomgil.itinerary.application.command.handler.AddPlacesToUnscheduledHandler;
import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceCommand;
import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceResult;
import com.soomgil.preference.application.command.handler.ApplyTripVotePreferenceCommandHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import com.soomgil.voting.application.command.dto.CloseVoteSessionCommand;
import com.soomgil.voting.application.command.dto.SubmitMyVoteCommand;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteItineraryLinkRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.application.service.CompleteVoteSessionService;
import com.soomgil.voting.application.service.VoteResultViewFactory;
import com.soomgil.voting.application.service.VoteSessionAssembler;
import com.soomgil.voting.application.service.VoteStickerPlacementService;
import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.time.Instant;
import com.soomgil.voting.application.port.VoteNotificationPublisher;
import com.soomgil.voting.application.port.VoteRealtimePublisher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VoteSessionCompletionTest {

	private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

	private final VoteNotificationPublisher notifications = mock(VoteNotificationPublisher.class);
	private final VoteRealtimePublisher realtimePublisher = mock(VoteRealtimePublisher.class);
	private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
	private final TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
	private final AddPlacesToUnscheduledHandler itineraryHandler = mock(AddPlacesToUnscheduledHandler.class);
	private final ApplyTripVotePreferenceCommandHandler preferenceHandler =
		mock(ApplyTripVotePreferenceCommandHandler.class);

	private final CompleteVoteSessionService completeService = new CompleteVoteSessionService(
		repository, itineraryHandler, preferenceHandler, () -> NOW, notifications, realtimePublisher
	);
	private final VoteSessionAssembler assembler = new VoteSessionAssembler();
	private final VoteStickerPlacementService placementService = new VoteStickerPlacementService(repository);

	private final SubmitMyVoteHandler submitHandler = new SubmitMyVoteHandler(
		repository, tripAccessGuard, placementService, completeService, assembler, () -> NOW, realtimePublisher
	);
	private final CloseVoteSessionHandler closeHandler = new CloseVoteSessionHandler(
		repository, tripAccessGuard, completeService, new VoteResultViewFactory()
	);

	private final UUID tripId = UUID.randomUUID();
	private final UUID sessionId = UUID.randomUUID();
	private final UUID ownerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID ownerParticipantId = UUID.randomUUID();
	private final UUID memberParticipantId = UUID.randomUUID();
	private final UUID candidateA = UUID.randomUUID();
	private final UUID candidateB = UUID.randomUUID();

	@BeforeEach
	void stubDefaults() {
		when(repository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(openSession()));
		when(repository.findById(sessionId)).thenReturn(Optional.of(openSession()));
		when(repository.findCandidates(sessionId)).thenReturn(List.of(
			candidate(candidateA, 1, "126508", "성산일출봉"),
			candidate(candidateB, 2, "126509", "우도")
		));
		when(repository.findParticipants(sessionId)).thenReturn(List.of(
			participant(ownerParticipantId, ownerId, VoteParticipantStatus.SUBMITTED),
			participant(memberParticipantId, memberId, VoteParticipantStatus.IN_PROGRESS)
		));
		when(repository.findParticipant(sessionId, memberId))
			.thenReturn(Optional.of(participant(memberParticipantId, memberId, VoteParticipantStatus.IN_PROGRESS)));
		when(repository.findStickersByParticipant(memberParticipantId)).thenReturn(List.of(
			sticker(memberParticipantId, candidateA, 3)
		));
		when(repository.findStickersBySession(sessionId)).thenReturn(List.of(
			sticker(ownerParticipantId, candidateA, 2),
			sticker(memberParticipantId, candidateA, 3)
		));
		when(repository.markParticipantSubmittedIfNotYet(eq(memberParticipantId), anyInt(), any(Instant.class)))
			.thenReturn(true);
		when(repository.completeIfOpen(eq(sessionId), any(), any(), any(Instant.class))).thenReturn(true);
		when(repository.markResultAppliedIfAbsent(eq(sessionId), any(Instant.class))).thenReturn(true);
		when(repository.findItineraryLinks(sessionId)).thenReturn(List.of());
		when(itineraryHandler.handle(any(AddPlacesToUnscheduledCommand.class))).thenReturn(
			new AddPlacesToUnscheduledResult(
				tripId, 12L, UUID.randomUUID(),
				List.of(new AddedUnscheduledPlace(UUID.randomUUID(), "KTO", "126508", "성산일출봉")),
				List.of()
			)
		);
		when(preferenceHandler.handle(any(ApplyTripVotePreferenceCommand.class)))
			.thenReturn(new ApplyTripVotePreferenceResult(1, 0));
	}

	@Test
	@DisplayName("마지막 참여자가 제출하면 자동으로 종료된다")
	void lastSubmissionCompletesSession() {
		when(repository.countSubmittedParticipants(sessionId)).thenReturn(2);

		submitHandler.handle(new SubmitMyVoteCommand(tripId, sessionId, memberId, null));

		verify(repository).completeIfOpen(eq(sessionId), eq(VoteCompletionReason.ALL_SUBMITTED), eq(null),
			any(Instant.class));
	}

	@Test
	@DisplayName("아직 제출하지 않은 참여자가 남아 있으면 종료하지 않는다")
	void nonLastSubmissionKeepsSessionOpen() {
		when(repository.countSubmittedParticipants(sessionId)).thenReturn(1);

		submitHandler.handle(new SubmitMyVoteCommand(tripId, sessionId, memberId, null));

		verify(repository, never()).completeIfOpen(any(UUID.class), any(), any(), any(Instant.class));
	}

	@Test
	@DisplayName("이중 제출은 VOTE_ALREADY_SUBMITTED로 거절한다")
	void doubleSubmissionIsRejected() {
		when(repository.markParticipantSubmittedIfNotYet(eq(memberParticipantId), anyInt(), any(Instant.class)))
			.thenReturn(false);

		assertThatThrownBy(() -> submitHandler.handle(new SubmitMyVoteCommand(tripId, sessionId, memberId, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_ALREADY_SUBMITTED));
	}

	@Test
	@DisplayName("종료된 세션에는 제출할 수 없다")
	void cannotSubmitToCompletedSession() {
		when(repository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(completedSession()));

		assertThatThrownBy(() -> submitHandler.handle(new SubmitMyVoteCommand(tripId, sessionId, memberId, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_SESSION_CLOSED));
	}

	@Test
	@DisplayName("참여자가 아니면 제출할 수 없다")
	void nonParticipantCannotSubmit() {
		UUID stranger = UUID.randomUUID();
		when(repository.findParticipant(sessionId, stranger)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> submitHandler.handle(new SubmitMyVoteCommand(tripId, sessionId, stranger, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_NOT_PARTICIPANT));
	}

	@Test
	@DisplayName("미투표자가 있는데 확인하지 않으면 조기 종료를 거절한다")
	void earlyCloseRequiresAcknowledgement() {
		assertThatThrownBy(() -> closeHandler.handle(
			new CloseVoteSessionCommand(tripId, sessionId, ownerId, false)
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_UNVOTED_PARTICIPANTS_NOT_ACKNOWLEDGED));

		verify(repository, never()).completeIfOpen(any(UUID.class), any(), any(), any(Instant.class));
	}

	@Test
	@DisplayName("방장이 경고를 확인하면 조기 종료된다")
	void ownerCanCloseEarlyAfterAcknowledgement() {
		when(repository.findById(sessionId)).thenReturn(Optional.of(completedSession()));

		TripVoteSessionResult result = closeHandler.handle(
			new CloseVoteSessionCommand(tripId, sessionId, ownerId, true)
		);

		assertThat(result.status()).isEqualTo(VoteSessionStatus.COMPLETED);
		verify(repository).completeIfOpen(eq(sessionId), eq(VoteCompletionReason.OWNER_EARLY_CLOSE), eq(ownerId),
			any(Instant.class));
	}

	@Test
	@DisplayName("이미 종료된 세션을 다시 종료해도 결과를 중복 반영하지 않는다")
	void closingCompletedSessionIsIdempotent() {
		when(repository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(completedSession()));
		when(repository.findById(sessionId)).thenReturn(Optional.of(completedSession()));

		TripVoteSessionResult result = closeHandler.handle(
			new CloseVoteSessionCommand(tripId, sessionId, ownerId, true)
		);

		assertThat(result.status()).isEqualTo(VoteSessionStatus.COMPLETED);
		verify(repository, never()).completeIfOpen(any(UUID.class), any(), any(), any(Instant.class));
		verify(itineraryHandler, never()).handle(any(AddPlacesToUnscheduledCommand.class));
		verify(preferenceHandler, never()).handle(any(ApplyTripVotePreferenceCommand.class));
		verify(notifications, never()).publish(any(), any(), anyBoolean(), any());
	}

	@Test
	@DisplayName("결과 반영 표시를 이미 가져간 경우 일정과 취향을 다시 반영하지 않는다")
	void resultApplicationRunsOnlyOnce() {
		when(repository.markResultAppliedIfAbsent(eq(sessionId), any(Instant.class))).thenReturn(false);

		completeService.complete(openSession(), VoteCompletionReason.ALL_SUBMITTED, null);

		verify(itineraryHandler, never()).handle(any(AddPlacesToUnscheduledCommand.class));
		verify(preferenceHandler, never()).handle(any(ApplyTripVotePreferenceCommand.class));
		verify(notifications, never()).publish(any(), any(), anyBoolean(), any());
	}

	@Test
	@DisplayName("마지막 제출과 방장 조기 종료가 동시에 와도 결과 반영은 한 번만 일어난다")
	void concurrentCompletionAppliesResultOnce() {
		when(repository.markResultAppliedIfAbsent(eq(sessionId), any(Instant.class)))
			.thenReturn(true)
			.thenReturn(false);

		completeService.complete(openSession(), VoteCompletionReason.ALL_SUBMITTED, null);
		completeService.complete(openSession(), VoteCompletionReason.OWNER_EARLY_CLOSE, ownerId);

		verify(notifications, times(1)).publish(tripId, sessionId, true, NOW);
		verify(itineraryHandler, times(1)).handle(any(AddPlacesToUnscheduledCommand.class));
		verify(preferenceHandler, times(2)).handle(any(ApplyTripVotePreferenceCommand.class));
	}

	@Test
	@DisplayName("선정된 관광지만 일차 미정 일정에 추가한다")
	void addsOnlySelectedPlacesToItinerary() {
		when(repository.findStickersBySession(sessionId)).thenReturn(List.of(
			sticker(ownerParticipantId, candidateA, 5),
			sticker(memberParticipantId, candidateB, 1)
		));

		completeService.complete(openSession(), VoteCompletionReason.ALL_SUBMITTED, null);

		ArgumentCaptor<AddPlacesToUnscheduledCommand> captor =
			ArgumentCaptor.forClass(AddPlacesToUnscheduledCommand.class);
		verify(itineraryHandler).handle(captor.capture());
		assertThat(captor.getValue().places()).hasSize(1);
		assertThat(captor.getValue().places().get(0).externalPlaceId()).isEqualTo("126508");
	}

	@Test
	@DisplayName("선정 여부와 무관하게 스티커를 붙인 모든 장소를 취향에 반영한다")
	void appliesPreferenceForEveryStickeredPlace() {
		when(repository.findStickersBySession(sessionId)).thenReturn(List.of(
			sticker(ownerParticipantId, candidateA, 5),
			sticker(memberParticipantId, candidateB, 1)
		));

		completeService.complete(openSession(), VoteCompletionReason.ALL_SUBMITTED, null);

		ArgumentCaptor<ApplyTripVotePreferenceCommand> captor =
			ArgumentCaptor.forClass(ApplyTripVotePreferenceCommand.class);
		verify(preferenceHandler, times(2)).handle(captor.capture());
		assertThat(captor.getAllValues())
			.flatExtracting(ApplyTripVotePreferenceCommand::places)
			.extracting(place -> place.externalPlaceId())
			.containsExactlyInAnyOrder("126508", "126509");
	}

	@Test
	@DisplayName("일정 반영 결과를 후보별 링크로 기록한다")
	void recordsItineraryLinkPerSelectedCandidate() {
		completeService.complete(openSession(), VoteCompletionReason.ALL_SUBMITTED, null);

		ArgumentCaptor<VoteItineraryLinkRecord> captor = ArgumentCaptor.forClass(VoteItineraryLinkRecord.class);
		verify(repository).insertItineraryLinkIfAbsent(captor.capture(), any(Instant.class));
		assertThat(captor.getValue().outcome()).isEqualTo("ADDED");
		assertThat(captor.getValue().candidateId()).isEqualTo(candidateA);
	}

	private VoteSessionRecord openSession() {
		return new VoteSessionRecord(
			sessionId, tripId, VoteSessionStatus.OPEN, ownerId, 5, 1, 2, NOW, null, null, null, null
		);
	}

	private VoteSessionRecord completedSession() {
		return new VoteSessionRecord(
			sessionId, tripId, VoteSessionStatus.COMPLETED, ownerId, 5, 1, 2, NOW, NOW,
			VoteCompletionReason.OWNER_EARLY_CLOSE, ownerId, NOW
		);
	}

	private VoteCandidateRecord candidate(UUID id, int sortOrder, String externalPlaceId, String name) {
		return new VoteCandidateRecord(
			id, sessionId, sortOrder, "KTO", externalPlaceId, name,
			"제주특별자치도", 33.45, 126.94, null, "A01", 0, false, null
		);
	}

	private VoteParticipantRecord participant(UUID id, UUID userId, VoteParticipantStatus status) {
		return new VoteParticipantRecord(
			id, sessionId, userId, status, 5, 0,
			status == VoteParticipantStatus.SUBMITTED ? NOW : null
		);
	}

	private VoteStickerRecord sticker(UUID participantId, UUID candidateId, int count) {
		return new VoteStickerRecord(UUID.randomUUID(), sessionId, participantId, candidateId, count);
	}
}
