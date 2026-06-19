package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;

/**
 * 관광 원천 저장소에서 장소 상세 정보를 조회하는 repository.
 */
public class TourismSourcePlaceDetailRepository {

	/**
	 * provider와 외부 장소 id에 맞는 관광 원천 상세 정보를 조회한다.
	 *
	 * @param query 상세 조회 query
	 * @return 장소 상세 조회 결과
	 */
	public PlaceDetailItem find(PlaceDetailQuery query) {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "Tourism source place detail repository is scaffolded only.");
	}
}
