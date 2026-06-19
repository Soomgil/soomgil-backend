package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.application.query.dto.PlaceSearchResult;

/**
 * 관광 원천 저장소에서 장소 검색 결과를 조회하는 repository.
 */
public class TourismSourcePlaceSearchRepository {

	/**
	 * 검색 조건에 맞는 관광 원천 장소 목록을 조회한다.
	 *
	 * @param criteria 검색 조건
	 * @return 검색 결과와 page metadata
	 */
	public PlaceSearchResult search(PlaceSearchCriteria criteria) {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "Tourism source place search repository is scaffolded only.");
	}
}
