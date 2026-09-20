package com.soomgil.place.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.soomgil.place.application.port.RegionViewport;

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

	/**
	 * 지역에 속한, 좌표가 있는 관광 원천 장소들의 경계와 중심을 구한다. gugunCode가 null이면 시도 전체.
	 * 한반도 밖(잘못된 0 등) 좌표는 제외한다.
	 */
	@Select("""
		SELECT avg(latitude) AS centerLat, avg(longitude) AS centerLng,
		       min(latitude) AS minLat, min(longitude) AS minLng,
		       max(latitude) AS maxLat, max(longitude) AS maxLng,
		       count(*) AS placeCount
		FROM tourism_source.attractions
		WHERE area_code = #{areaCode}
		  AND (#{gugunCode} IS NULL OR si_gun_gu_code = #{gugunCode})
		  AND latitude BETWEEN 33 AND 39
		  AND longitude BETWEEN 124 AND 132
		""")
	@ConstructorArgs({
		@Arg(column = "centerLat", javaType = double.class),
		@Arg(column = "centerLng", javaType = double.class),
		@Arg(column = "minLat", javaType = double.class),
		@Arg(column = "minLng", javaType = double.class),
		@Arg(column = "maxLat", javaType = double.class),
		@Arg(column = "maxLng", javaType = double.class),
		@Arg(column = "placeCount", javaType = long.class)
	})
	RegionViewport findRegionViewport(@Param("areaCode") int areaCode, @Param("gugunCode") Integer gugunCode);
}
