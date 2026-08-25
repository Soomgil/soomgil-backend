package com.soomgil.voting.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import com.soomgil.voting.application.command.dto.CloseVoteSessionCommand;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.service.CompleteVoteSessionService;
import com.soomgil.voting.application.service.VoteResultViewFactory;
import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CloseVoteSessionCommand}를 처리해 방장이 투표를 조기 종료한다.
 *
 * <p>아직 제출하지 않은 참여자가 있으면 방장이 경고를 확인했다는 표시({@code acknowledgeUnvotedParticipants})가
 * 있어야 종료할 수 있다. 확인 없이 요청하면 {@code VOTE_UNVOTED_PARTICIPANTS_NOT_ACKNOWLEDGED}로 거절한다.
 *
 * <p>이미 종료된 세션에 대해 호출하면 실패가 아니라 기존 결과를 그대로 반환한다. 종료 전이와 결과 반영이
 * 모두 조건부 UPDATE로 보호되므로 재시도해도 일정과 취향이 중복 반영되지 않는다.
 */
@Component
public class CloseVoteSessionHandler implements CommandHandler<CloseVoteSessionCommand, TripVoteSessionResult> {

	private final VoteSessionRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final CompleteVoteSessionService completeVoteSessionService;
	private final VoteResultViewFactory resultViewFactory;

	public CloseVoteSessionHandler(
		VoteSessionRepository repository,
		TripAccessGuard tripAccessGuard,
		CompleteVoteSessionService completeVoteSessionService,
		VoteResultViewFactory resultViewFactory
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.completeVoteSessionService =
			Objects.requireNonNull(completeVoteSessionService, "completeVoteSessionService must not be null");
		this.resultViewFactory = Objects.requireNonNull(resultViewFactory, "resultViewFactory must not be null");
	}

	@Override
	@Transactional
	public TripVoteSessionResult handle(CloseVoteSessionCommand command) {
		tripAccessGuard.requireOwner(command.tripId(), command.actorUserId());

		VoteSessionRecord session = repository.findByIdForUpdate(command.sessionId())
			.filter(record -> record.tripId().equals(command.tripId()))
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_SESSION_NOT_FOUND, "Vote session was not found."
			));

		if (session.status() == VoteSessionStatus.OPEN) {
			requireAcknowledgementWhenUnvotedExists(command, session);
			completeVoteSessionService.complete(
				session, VoteCompletionReason.OWNER_EARLY_CLOSE, command.actorUserId()
			);
		}

		VoteSessionRecord completed = repository.findById(session.id())
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_SESSION_NOT_FOUND, "Vote session was not found."
			));
		return resultViewFactory.create(
			completed,
			repository.findCandidates(completed.id()),
			repository.findItineraryLinks(completed.id()),
			null,
			null
		);
	}

	private void requireAcknowledgementWhenUnvotedExists(
		CloseVoteSessionCommand command,
		VoteSessionRecord session
	) {
		if (command.acknowledgeUnvotedParticipants()) {
			return;
		}
		boolean hasUnvoted = repository.findParticipants(session.id()).stream()
			.map(VoteParticipantRecord::status)
			.anyMatch(status -> status != VoteParticipantStatus.SUBMITTED);
		if (hasUnvoted) {
			throw new BusinessException(
				ErrorCode.VOTE_UNVOTED_PARTICIPANTS_NOT_ACKNOWLEDGED,
				"Owner must acknowledge participants who have not voted."
			);
		}
	}
}
