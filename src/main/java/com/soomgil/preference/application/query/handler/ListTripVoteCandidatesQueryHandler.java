package com.soomgil.preference.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.preference.application.query.dto.ListTripVoteCandidatesQuery;
import com.soomgil.preference.application.query.dto.TripVoteCandidateView;
import java.util.List;

/**
 * 투표 후보 생성 query를 처리하는 application 계약.
 *
 * <p>투표 모듈은 이 interface만 의존하고 preference DB나 mapper를 직접 읽지 않는다.
 */
public interface ListTripVoteCandidatesQueryHandler
	extends QueryHandler<ListTripVoteCandidatesQuery, List<TripVoteCandidateView>> {
}
