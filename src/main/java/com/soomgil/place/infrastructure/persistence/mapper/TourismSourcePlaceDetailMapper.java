package com.soomgil.place.infrastructure.persistence.mapper;

import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceDetailRow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 관광 원천 장소 상세 SQL mapper.
 */
@Mapper
public interface TourismSourcePlaceDetailMapper {

	/**
	 * KTO content id로 관광 원천 장소 상세 row를 조회한다.
	 *
	 * @param contentId KTO content id 문자열
	 * @return 장소 상세 row, 없으면 {@code null}
	 */
	TourismSourcePlaceDetailRow findByContentId(String contentId);
}
