package com.soomgil.place.infrastructure.persistence.mapper;

import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateCriteria;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceSearchRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 관광 원천 장소 검색 SQL mapper.
 */
@Mapper
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

	/**
	 * 지도 viewport 안의 관광 원천 장소 후보를 조회한다.
	 *
	 * @param criteria viewport 후보 조회 조건
	 * @return 후보 row 목록
	 */
	List<TourismSourcePlaceSearchRow> findViewportCandidates(PlaceViewportCandidateCriteria criteria);
}
