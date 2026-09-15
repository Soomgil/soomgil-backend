package com.soomgil.voting.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.MyVoteStickerState;
import com.soomgil.voting.application.command.dto.SaveMyVoteStickersCommand;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.service.VoteSessionAssembler;
import com.soomgil.voting.application.service.VoteStickerPlacementService;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SaveMyVoteStickersCommand}를 처리해 제출 전 스티커 배치를 저장한다.
 *
 * <p>세션 row에 잠금을 걸어 같은 참여자의 동시 저장이 엇갈리지 않게 한다.
 * 종료된 세션에 저장하려 하면 {@code VOTE_SESSION_CLOSED}, 참여자가 아니면 {@code VOTE_NOT_PARTICIPANT}다.
 */
@Component
public class SaveMyVoteStickersHandler implements CommandHandler<SaveMyVoteStickersCommand, MyVoteStickerState> {

	private final VoteSessionRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final VoteStickerPlacementService placementService;
	private final VoteSessionAssembler assembler;
	private final TimeProvider timeProvider;

	public SaveMyVoteStickersHandler(
		VoteSessionRepository repository,
		TripAccessGuard tripAccessGuard,
		VoteStickerPlacementService placementService,
		VoteSessionAssembler assembler,
		TimeProvider timeProvider
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.placementService = Objects.requireNonNull(placementService, "placementService must not be null");
		this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public MyVoteStickerState handle(SaveMyVoteStickersCommand command) {
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
				ErrorCode.VOTE_NOT_PARTICIPANT, "Only confirmed vote participants can place stickers."
			));

		Instant now = timeProvider.now();
		placementService.replacePlacements(session, participant, command.placements(), now);

		VoteParticipantRecord updated = repository.findParticipant(session.id(), command.actorUserId())
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_NOT_PARTICIPANT, "Vote participant was not found."
			));
		return new MyVoteStickerState(
			session.id(),
			assembler.toParticipation(updated, repository.findStickersByParticipant(updated.id()))
		);
	}
}
