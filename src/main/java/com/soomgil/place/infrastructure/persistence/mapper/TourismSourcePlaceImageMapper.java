package com.soomgil.place.infrastructure.persistence.mapper;

import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceImageRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 관광 원천 장소 이미지 후보 SQL mapper.
 */
@Mapper
public interface TourismSourcePlaceImageMapper {

	/**
	 * KTO content id로 일반 관광지 이미지 후보를 조회한다.
	 *
	 * @param contentId KTO content id 문자열
	 * @return 일반 이미지 후보 row 목록
	 */
	List<TourismSourcePlaceImageRow> findNormalImages(@Param("contentId") String contentId);

	/**
	 * KTO content id로 public serving 가능한 수상작 이미지 후보 1장을 조회한다.
	 *
	 * @param contentId KTO content id 문자열
	 * @return 수상작 이미지 후보 row, 없으면 {@code null}
	 */
	TourismSourcePlaceImageRow findAwardImage(@Param("contentId") String contentId);
}
