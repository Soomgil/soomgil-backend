package com.soomgil.place.application.query.handler;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import org.springframework.stereotype.Component;

/**
 * persistence 구현 전까지 상세 endpoint scaffold 상태를 유지하는 기본 handler.
 */
@Component
public class ScaffoldPlaceDetailQueryHandler implements PlaceDetailQueryHandler {

	@Override
	public PlaceDetail handle(PlaceDetailQuery query) {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "Place detail is scaffolded only.");
	}
}
