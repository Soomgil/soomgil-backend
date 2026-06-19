package com.soomgil.place.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.place.application.query.dto.PlaceImageCandidate;
import com.soomgil.place.application.query.dto.PlaceImageCandidateQuery;
import java.util.List;

/**
 * 장소 이미지 후보 query를 처리하는 application 계약.
 */
public interface PlaceImageCandidateQueryHandler extends QueryHandler<PlaceImageCandidateQuery, List<PlaceImageCandidate>> {
}
