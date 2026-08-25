package com.soomgil.voting.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.preference.application.query.dto.ListTripVoteCandidatesQuery;
import com.soomgil.preference.application.query.dto.TripVoteCandidateView;
import com.soomgil.preference.application.query.handler.ListTripVoteCandidatesQueryHandler;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.TripVoteSessionDetail;
import com.soomgil.voting.application.command.dto.OpenVoteSessionCommand;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.service.VoteSessionAssembler;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import com.soomgil.voting.domain.policy.VoteSessionPolicy;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link OpenVoteSessionCommand}를 처리해 투표를 시작한다.
 *
 * <p>처리 순서는 방장 권한 확인 → 진행 중 세션 확인 → 후보 생성 → 설정값 검증 → 세션/후보/참여자 저장이다.
 *
 * <p>투표 시작 시점의 활성 여행방 멤버 전원이 참여자로 확정되고, 추천 후보는 snapshot으로 고정되어
 * 투표 도중 추천 결과가 바뀌어도 변하지 않는다. 지급 개수와 선정 개수는 실제로 생성된 후보 수를 기준으로
 * 검증하며, 시작 이후에는 변경할 수 없다.
 */
@Component
public class OpenVoteSessionHandler implements CommandHandler<OpenVoteSessionCommand, TripVoteSessionDetail> {

	private final VoteSessionRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final ListTripMembersHandler membersHandler;
	private final FindTripDetailHandler tripDetailHandler;
	private final ListTripVoteCandidatesQueryHandler candidatesHandler;
	private final VoteSessionAssembler assembler;
	private final TimeProvider timeProvider;

	public OpenVoteSessionHandler(
		VoteSessionRepository repository,
		TripAccessGuard tripAccessGuard,
		ListTripMembersHandler membersHandler,
		FindTripDetailHandler tripDetailHandler,
		ListTripVoteCandidatesQueryHandler candidatesHandler,
		VoteSessionAssembler assembler,
		TimeProvider timeProvider
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.membersHandler = Objects.requireNonNull(membersHandler, "membersHandler must not be null");
		this.tripDetailHandler = Objects.requireNonNull(tripDetailHandler, "tripDetailHandler must not be null");
		this.candidatesHandler = Objects.requireNonNull(candidatesHandler, "candidatesHandler must not be null");
		this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public TripVoteSessionDetail handle(OpenVoteSessionCommand command) {
		tripAccessGuard.requireOwner(command.tripId(), command.actorUserId());

		if (repository.findActiveByTripId(command.tripId()).isPresent()) {
			throw new BusinessException(
				ErrorCode.VOTE_SESSION_ALREADY_OPEN, "Trip already has an active vote session."
			);
		}

		List<TripMemberView> members = membersHandler.handle(new ListTripMembersQuery(
			command.tripId(), command.actorUserId(), TripMemberStatus.ACTIVE
		));
		if (members.isEmpty()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vote requires at least one participant.");
		}

		int requestedCandidateCount = command.candidateCount() == null
			? VoteSessionPolicy.DEFAULT_CANDIDATE_COUNT
			: command.candidateCount();
		// 여행방에 등록된 지역이 없을 때를 대비해 대표 목적지를 대체 검색어로 함께 넘긴다.
		TripDetailView trip = tripDetailHandler.handle(
			new FindTripDetailQuery(command.tripId(), command.actorUserId())
		);
		List<TripVoteCandidateView> candidateViews = candidatesHandler.handle(new ListTripVoteCandidatesQuery(
			command.tripId(), command.actorUserId(), requestedCandidateCount, trip.displayDestination()
		));
		int candidateCount = candidateViews.size();
		if (candidateCount < command.selectionCount() || candidateCount < 1) {
			throw new BusinessException(
				ErrorCode.VOTE_CANDIDATE_POOL_INSUFFICIENT,
				"Recommendation produced " + candidateCount + " candidates for selection count "
					+ command.selectionCount() + "."
			);
		}
		if (!VoteSessionPolicy.isValidStickerAllowance(command.stickerAllowance(), candidateCount)) {
			throw new BusinessException(
				ErrorCode.VALIDATION_FAILED,
				"Sticker allowance must be between 1 and the candidate count."
			);
		}
		if (!VoteSessionPolicy.isValidSelectionCount(command.selectionCount(), candidateCount)) {
			throw new BusinessException(
				ErrorCode.VALIDATION_FAILED,
				"Selection count must be between 1 and the candidate count."
			);
		}

		Instant now = timeProvider.now();
		UUID sessionId = Ids.newUuid();
		VoteSessionRecord session = new VoteSessionRecord(
			sessionId,
			command.tripId(),
			VoteSessionStatus.OPEN,
			command.actorUserId(),
			command.stickerAllowance(),
			command.selectionCount(),
			candidateCount,
			now,
			null,
			null,
			null,
			null
		);
		repository.insertSession(session);

		List<VoteCandidateRecord> candidates = new ArrayList<>();
		for (TripVoteCandidateView view : candidateViews) {
			candidates.add(new VoteCandidateRecord(
				Ids.newUuid(),
				sessionId,
				view.rank(),
				view.provider() == null ? null : view.provider().name(),
				view.externalPlaceId(),
				view.name(),
				view.address(),
				view.lat(),
				view.lng(),
				view.thumbnailUrl() == null ? null : view.thumbnailUrl().toString(),
				view.category(),
				0,
				false,
				null
			));
		}
		repository.insertCandidates(candidates);

		List<VoteParticipantRecord> participants = new ArrayList<>();
		for (TripMemberView member : members) {
			participants.add(new VoteParticipantRecord(
				Ids.newUuid(),
				sessionId,
				member.userId(),
				VoteParticipantStatus.NOT_STARTED,
				command.stickerAllowance(),
				0,
				null
			));
		}
		repository.insertParticipants(participants);

		return assembler.toDetail(session, candidates, participants);
	}
}
