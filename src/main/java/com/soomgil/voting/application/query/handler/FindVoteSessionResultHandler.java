package com.soomgil.voting.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.query.dto.FindVoteSessionResultQuery;
import com.soomgil.voting.application.service.VoteResultViewFactory;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FindVoteSessionResultQuery}를 처리해 종료된 투표의 결과를 조회한다.
 *
 * <p>active member에게만 노출한다. 아직 진행 중인 세션의 결과를 조회하면 중간 집계가 새어 나가므로
 * {@code VOTE_SESSION_NOT_FOUND}로 응답한다.
 */
@Component
public class FindVoteSessionResultHandler
	implements QueryHandler<FindVoteSessionResultQuery, TripVoteSessionResult> {

	private final VoteSessionRepository repository;
	private final TripAccessGuard tripAccessGuard;
	private final VoteResultViewFactory resultViewFactory;

	public FindVoteSessionResultHandler(
		VoteSessionRepository repository,
		TripAccessGuard tripAccessGuard,
		VoteResultViewFactory resultViewFactory
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.resultViewFactory = Objects.requireNonNull(resultViewFactory, "resultViewFactory must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public TripVoteSessionResult handle(FindVoteSessionResultQuery query) {
		tripAccessGuard.requireActiveMember(query.tripId(), query.userId());

		VoteSessionRecord session = repository.findById(query.sessionId())
			.filter(record -> record.tripId().equals(query.tripId()))
			.filter(record -> record.completedAt() != null)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VOTE_SESSION_NOT_FOUND, "Completed vote session was not found."
			));

		return resultViewFactory.create(
			session,
			repository.findCandidates(session.id()),
			repository.findItineraryLinks(session.id()),
			null,
			null
		);
	}
}
