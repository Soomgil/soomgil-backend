package com.soomgil.voting.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.TripVoteSessionState;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.query.dto.FindCurrentVoteSessionQuery;
import com.soomgil.voting.application.service.VoteSessionAssembler;
import com.soomgil.voting.domain.model.VoteNextScreen;
import com.soomgil.voting.domain.policy.VoteSessionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FindCurrentVoteSessionQuery}를 처리해 여행 방 진입 시 보여줄 화면을 판정한다.
 *
 * <p>진행 중인 세션이 있으면 그 세션을, 없으면 가장 최근 세션을 기준으로 판정한다.
 * 세션이 아예 없거나 요청자가 참여자가 아니면 지도 화면으로 안내한다.
 */
@Component
public class FindCurrentVoteSessionHandler
	implements QueryHandler<FindCurrentVoteSessionQuery, TripVoteSessionState> {

	private final VoteSessionRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final VoteSessionAssembler assembler;

	public FindCurrentVoteSessionHandler(
		VoteSessionRepository repository,
		TripAccessGuard tripAccessGuard,
		VoteSessionAssembler assembler
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public TripVoteSessionState handle(FindCurrentVoteSessionQuery query) {
		tripAccessGuard.requireActiveMember(query.tripId(), query.userId());

		Optional<VoteSessionRecord> found = repository.findActiveByTripId(query.tripId())
			.or(() -> repository.findLatestByTripId(query.tripId()));
		if (found.isEmpty()) {
			return new TripVoteSessionState(false, VoteNextScreen.MAP, null, null);
		}

		VoteSessionRecord session = found.get();
		List<VoteParticipantRecord> participants = repository.findParticipants(session.id());
		VoteParticipantRecord participant = repository.findParticipant(session.id(), query.userId()).orElse(null);

		return new TripVoteSessionState(
			true,
			VoteSessionPolicy.nextScreen(session.status(), participant == null ? null : participant.status()),
			assembler.toDetail(session, repository.findCandidates(session.id()), participants),
			assembler.toParticipation(
				participant,
				participant == null ? List.of() : repository.findStickersByParticipant(participant.id())
			)
		);
	}
}
