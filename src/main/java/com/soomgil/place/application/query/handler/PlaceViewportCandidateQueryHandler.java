package com.soomgil.place.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateQuery;
import java.util.List;

/**
 * 지도 viewport 장소 후보 query를 처리하는 application 계약.
 */
public interface PlaceViewportCandidateQueryHandler
	extends QueryHandler<PlaceViewportCandidateQuery, List<PlaceViewportCandidate>> {
}
