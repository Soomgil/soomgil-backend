package com.soomgil.place.application.query.handler;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import org.springframework.stereotype.Component;

/**
 * persistence 구현 전까지 검색 endpoint scaffold 상태를 유지하는 기본 handler.
 */
@Component
public class ScaffoldPlaceSearchQueryHandler implements PlaceSearchQueryHandler {

	@Override
	public PagedPlaceSummary handle(PlaceSearchQuery query) {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "Place search is scaffolded only.");
	}
}
