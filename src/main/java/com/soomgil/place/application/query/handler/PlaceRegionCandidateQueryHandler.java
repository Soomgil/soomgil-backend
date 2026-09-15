package com.soomgil.place.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.place.application.query.dto.PlaceRegionCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import java.util.List;

/**
 * 여행 지역 기반 장소 후보 query를 처리하는 application 계약.
 */
public interface PlaceRegionCandidateQueryHandler
	extends QueryHandler<PlaceRegionCandidateQuery, List<PlaceViewportCandidate>> {
}
