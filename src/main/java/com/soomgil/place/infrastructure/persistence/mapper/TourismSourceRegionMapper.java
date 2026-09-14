package com.soomgil.place.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 관광 원천 시도·시군구 코드 조회 SQL mapper.
 */
@Mapper
public interface TourismSourceRegionMapper {
	/**
	 * 시도 안에서 이름이 같은 시군구 코드를 찾는다. 공백 차이는 무시한다.
	 *
	 * @param sidoCode KTO 시도 코드
	 * @param gugunName 시군구 이름
	 * @return 시군구 코드. 없으면 null
	 */
	@Select("""
		SELECT gugun_code
		FROM tourism_source.guguns
		WHERE sido_code = #{sidoCode}
		  AND replace(gugun_name, ' ', '') = replace(#{gugunName}, ' ', '')
		ORDER BY gugun_code
		LIMIT 1
		""")
	Integer findGugunCode(@Param("sidoCode") int sidoCode, @Param("gugunName") String gugunName);
}
