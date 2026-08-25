package com.soomgil.voting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.voting.application.command.dto.VoteStickerPlacementCommand;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VoteStickerPlacementServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

	private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
	private final VoteStickerPlacementService service = new VoteStickerPlacementService(repository);

	private final UUID sessionId = UUID.randomUUID();
	private final UUID participantId = UUID.randomUUID();
	private final UUID candidateA = UUID.randomUUID();
	private final UUID candidateB = UUID.randomUUID();

	@BeforeEach
	void stubCandidates() {
		when(repository.findCandidates(sessionId)).thenReturn(List.of(
			candidate(candidateA, 1),
			candidate(candidateB, 2)
		));
	}

	@Test
	@DisplayName("한 관광지에 스티커를 몰아붙일 수 있다")
	void allowsStackingStickersOnOnePlace() {
		int used = service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(new VoteStickerPlacementCommand(candidateA, 5)), NOW
		);

		assertThat(used).isEqualTo(5);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<VoteStickerRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).insertStickers(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).stickerCount()).isEqualTo(5);
	}

	@Test
	@DisplayName("사용한 스티커 총합이 지급량을 넘으면 거절한다")
	void rejectsUsageAboveAllowance() {
		assertThatThrownBy(() -> service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(
				new VoteStickerPlacementCommand(candidateA, 3),
				new VoteStickerPlacementCommand(candidateB, 3)
			), NOW
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_STICKER_ALLOWANCE_EXCEEDED));

		verify(repository, never()).insertStickers(any());
	}

	@Test
	@DisplayName("배치는 전체 치환이므로 목록에서 빠진 후보의 스티커는 회수된다")
	void replacingPlacementsWithdrawsMissingCandidates() {
		service.replacePlacements(
			session(), participant(VoteParticipantStatus.IN_PROGRESS, 5),
			List.of(new VoteStickerPlacementCommand(candidateB, 2)), NOW
		);

		verify(repository).deleteStickersByParticipant(participantId);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<VoteStickerRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).insertStickers(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).candidateId()).isEqualTo(candidateB);
	}

	@Test
	@DisplayName("스티커를 모두 회수하면 상태가 NOT_STARTED로 돌아간다")
	void withdrawingAllStickersResetsStatus() {
		service.replacePlacements(
			session(), participant(VoteParticipantStatus.IN_PROGRESS, 5), List.of(), NOW
		);

		verify(repository).updateParticipantProgress(
			eq(participantId), eq(VoteParticipantStatus.NOT_STARTED), eq(0), any(Instant.class)
		);
	}

	@Test
	@DisplayName("스티커를 붙이면 상태가 IN_PROGRESS가 된다")
	void placingStickersMarksInProgress() {
		service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(new VoteStickerPlacementCommand(candidateA, 2)), NOW
		);

		verify(repository).updateParticipantProgress(
			eq(participantId), eq(VoteParticipantStatus.IN_PROGRESS), eq(2), any(Instant.class)
		);
	}

	@Test
	@DisplayName("제출한 뒤에는 스티커를 수정할 수 없다")
	void cannotChangeAfterSubmission() {
		assertThatThrownBy(() -> service.replacePlacements(
			session(), participant(VoteParticipantStatus.SUBMITTED, 5),
			List.of(new VoteStickerPlacementCommand(candidateA, 1)), NOW
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_ALREADY_SUBMITTED));

		verify(repository, never()).deleteStickersByParticipant(any(UUID.class));
	}

	@Test
	@DisplayName("다른 세션의 후보에는 스티커를 붙일 수 없다")
	void rejectsCandidateFromAnotherSession() {
		assertThatThrownBy(() -> service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(new VoteStickerPlacementCommand(UUID.randomUUID(), 1)), NOW
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_CANDIDATE_NOT_FOUND));
	}

	@Test
	@DisplayName("0개나 음수 배치는 거절한다")
	void rejectsNonPositivePlacementCount() {
		assertThatThrownBy(() -> service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(new VoteStickerPlacementCommand(candidateA, 0)), NOW
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));

		assertThatThrownBy(() -> service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(new VoteStickerPlacementCommand(candidateA, -2)), NOW
		))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("같은 후보가 여러 번 들어오면 개수를 합산한다")
	void mergesDuplicateCandidateEntries() {
		int used = service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(
				new VoteStickerPlacementCommand(candidateA, 2),
				new VoteStickerPlacementCommand(candidateA, 1)
			), NOW
		);

		assertThat(used).isEqualTo(3);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<VoteStickerRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).insertStickers(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).stickerCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("지급량을 정확히 다 쓰는 배치는 허용한다")
	void allowsUsingExactAllowance() {
		int used = service.replacePlacements(
			session(), participant(VoteParticipantStatus.NOT_STARTED, 5),
			List.of(
				new VoteStickerPlacementCommand(candidateA, 3),
				new VoteStickerPlacementCommand(candidateB, 2)
			), NOW
		);

		assertThat(used).isEqualTo(5);
		verify(repository).updateParticipantProgress(
			eq(participantId), eq(VoteParticipantStatus.IN_PROGRESS), eq(5), any(Instant.class)
		);
	}

	private VoteSessionRecord session() {
		return new VoteSessionRecord(
			sessionId, UUID.randomUUID(), VoteSessionStatus.OPEN, UUID.randomUUID(),
			5, 2, 2, NOW, null, null, null, null
		);
	}

	private VoteParticipantRecord participant(VoteParticipantStatus status, int allowance) {
		return new VoteParticipantRecord(
			participantId, sessionId, UUID.randomUUID(), status, allowance, 0,
			status == VoteParticipantStatus.SUBMITTED ? NOW : null
		);
	}

	private VoteCandidateRecord candidate(UUID id, int sortOrder) {
		return new VoteCandidateRecord(
			id, sessionId, sortOrder, "KTO", "place-" + sortOrder, "장소 " + sortOrder,
			null, null, null, null, null, 0, false, null
		);
	}
}
