package com.soomgil.preference.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;

/**
 * 여행방 장소 추천 query를 처리하는 application 계약.
 */
public interface ListPlaceRecommendationsQueryHandler
	extends QueryHandler<ListPlaceRecommendationsQuery, PagedPlaceRecommendation> {
}
