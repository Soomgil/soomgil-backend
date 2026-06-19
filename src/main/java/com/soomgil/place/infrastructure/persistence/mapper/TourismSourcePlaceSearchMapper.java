package com.soomgil.place.infrastructure.persistence.mapper;

import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceSearchRow;
import java.util.List;

/**
 * 관광 원천 장소 검색 SQL mapper.
 */
public interface TourismSourcePlaceSearchMapper {

	/**
	 * 검색 조건에 맞는 관광 원천 장소 수를 조회한다.
	 *
	 * @param criteria 검색 조건
	 * @return 전체 검색 결과 수
	 */
	long count(PlaceSearchCriteria criteria);

	/**
	 * 검색 조건에 맞는 관광 원천 장소 page를 조회한다.
	 *
	 * @param criteria 검색 조건
	 * @return 검색 결과 row 목록
	 */
	List<TourismSourcePlaceSearchRow> search(PlaceSearchCriteria criteria);
}
