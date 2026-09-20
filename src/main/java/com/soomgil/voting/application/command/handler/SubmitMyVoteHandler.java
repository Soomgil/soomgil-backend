package com.soomgil.voting.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.TripVoteSessionState;
import com.soomgil.voting.application.command.dto.SubmitMyVoteCommand;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.port.VoteRealtimePublisher;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.application.service.CompleteVoteSessionService;
import com.soomgil.voting.application.service.VoteSessionAssembler;
import com.soomgil.voting.application.service.VoteStickerPlacementService;
import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import com.soomgil.voting.domain.policy.VoteSessionPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SubmitMyVoteCommand}를 처리해 투표를 제출한다.
 *
 * <p>제출은 {@code markParticipantSubmittedIfNotYet}의 조건부 UPDATE로만 기록되므로 이중 제출은
 * {@code VOTE_ALREADY_SUBMITTED}로 거절된다.
 *
 * <p>이 제출로 모든 활성 참여자가 제출을 마치면 같은 transaction 안에서 세션을 자동 종료하고
 * 결과를 확정한다. 종료 전이 자체는 조건부 UPDATE이므로 방장 조기 종료와 동시에 실행되어도
 * 실제 종료는 한 번만 일어난다.
 */
@Component
public class SubmitMyVoteHandler implements CommandHandler<SubmitMyVoteCommand, TripVoteSessionState> {

	private final VoteSessionRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final VoteStickerPlacementService placementService;
	private final CompleteVoteSessionService completeVoteSessionService;
	private final VoteSessionAssembler assembler;
	private final TimeProvider timeProvider;
	private final VoteRealtimePublisher realtimePublisher;

	public SubmitMyVoteHandler(
		VoteSessionRepository repository,
		TripAccessGuard tripAccessGuard,
		VoteStickerPlacementService placementService,
		CompleteVoteSessionService completeVoteSessionService,
		VoteSessionAssembler assembler,
		TimeProvider timeProvider,
		VoteRealtimePublisher realtimePublisher
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.placementService = Objects.requireNonNull(placementService, "placementService must not be null");
		this.completeVoteSessionService =
			Objects.requireNonNull(completeVoteSessionService, "completeVoteSessionService must not be null");
		this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.realtimePublisher = Objects.requireNonNull(realtimePublisher, "realtimePublisher must not be null");
	}

	@Override
	@Transactional
	public TripVoteSessionState handle(SubmitMyVoteCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());

		VoteSessionRecord session = repository.findByIdForUpdate(command.sessionId())
			.filter(record -> record.tripId().equals(command.tripId()))
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_SESSION_NOT_FOUND, "Vote session was not found."
			));
		if (session.status() != VoteSessionStatus.OPEN) {
			throw new BusinessException(ErrorCode.VOTE_SESSION_CLOSED, "Vote session is already completed.");
		}

		VoteParticipantRecord participant = repository
			.findParticipant(session.id(), command.actorUserId())
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_NOT_PARTICIPANT, "Only confirmed vote participants can submit."
			));

		Instant now = timeProvider.now();
		int usedStickerCount = command.placements() == null
			? currentUsedStickerCount(participant)
			: placementService.replacePlacements(session, participant, command.placements(), now);

		if (!repository.markParticipantSubmittedIfNotYet(participant.id(), usedStickerCount, now)) {
			throw new BusinessException(
				ErrorCode.VOTE_ALREADY_SUBMITTED, "Vote was already submitted and cannot be changed."
			);
		}

		if (!autoCompleteIfEveryoneSubmitted(session)) {
			realtimePublisher.publish(session.tripId(), session.id(), VoteSessionStatus.OPEN);
		}
		return currentState(session.id(), command.actorUserId());
	}

	private int currentUsedStickerCount(VoteParticipantRecord participant) {
		return repository.findStickersByParticipant(participant.id()).stream()
			.mapToInt(VoteStickerRecord::stickerCount)
			.sum();
	}

	private boolean autoCompleteIfEveryoneSubmitted(VoteSessionRecord session) {
		int total = repository.findParticipants(session.id()).size();
		int submitted = repository.countSubmittedParticipants(session.id());
		if (VoteSessionPolicy.shouldAutoComplete(submitted, total)) {
			completeVoteSessionService.complete(session, VoteCompletionReason.ALL_SUBMITTED, null);
			return true;
		}
		return false;
	}

	private TripVoteSessionState currentState(java.util.UUID sessionId, java.util.UUID userId) {
		VoteSessionRecord session = repository.findById(sessionId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_SESSION_NOT_FOUND, "Vote session was not found."
			));
		List<VoteParticipantRecord> participants = repository.findParticipants(sessionId);
		VoteParticipantRecord participant = repository.findParticipant(sessionId, userId).orElse(null);
		return new TripVoteSessionState(
			true,
			VoteSessionPolicy.nextScreen(
				session.status(),
				participant == null ? null : participant.status()
			),
			assembler.toDetail(session, repository.findCandidates(sessionId), participants, repository.findRegions(sessionId)),
			assembler.toParticipation(
				participant,
				participant == null ? List.of() : repository.findStickersByParticipant(participant.id())
			)
		);
	}
}
