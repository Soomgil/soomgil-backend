package com.soomgil.place.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;

/**
 * 관광지 검색 query를 처리하는 application 계약.
 */
public interface PlaceSearchQueryHandler extends QueryHandler<PlaceSearchQuery, PagedPlaceSummary> {
}
