package com.soomgil.preference.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;

/**
 * 전역 스와이프 feed query를 처리하는 application 계약.
 */
public interface SwipeFeedQueryHandler extends QueryHandler<SwipeFeedQuery, SwipeFeedResponse> {
}
