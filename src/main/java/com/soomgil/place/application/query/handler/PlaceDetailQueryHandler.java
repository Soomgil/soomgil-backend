package com.soomgil.place.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;

/**
 * 관광지 상세 query를 처리하는 application 계약.
 */
public interface PlaceDetailQueryHandler extends QueryHandler<PlaceDetailQuery, PlaceDetail> {
}
