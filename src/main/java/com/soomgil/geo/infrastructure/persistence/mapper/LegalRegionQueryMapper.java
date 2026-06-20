package com.soomgil.geo.infrastructure.persistence.mapper;

import com.soomgil.geo.infrastructure.persistence.row.LegalRegionRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 법정동 지역 읽기 SQL mapper.
 */
@Mapper
public interface LegalRegionQueryMapper {

	/**
	 * 법정동 지역 목록을 조회한다.
	 *
	 * @param query 이름 또는 전체 이름 검색어
	 * @param level 선택적 지역 level
	 * @param parentCode 선택적 상위 지역 code
	 * @param isActive 선택적 활성 상태
	 * @param size page 크기
	 * @param offset 시작 offset
	 * @return 지역 목록
	 */
	List<LegalRegionRow> findLegalRegions(
		@Param("query") String query,
		@Param("level") String level,
		@Param("parentCode") String parentCode,
		@Param("isActive") Boolean isActive,
		@Param("size") int size,
		@Param("offset") int offset
	);

	/**
	 * 법정동 지역 목록 조건에 맞는 전체 개수를 조회한다.
	 *
	 * @param query 이름 또는 전체 이름 검색어
	 * @param level 선택적 지역 level
	 * @param parentCode 선택적 상위 지역 code
	 * @param isActive 선택적 활성 상태
	 * @return 전체 개수
	 */
	long countLegalRegions(
		@Param("query") String query,
		@Param("level") String level,
		@Param("parentCode") String parentCode,
		@Param("isActive") Boolean isActive
	);
}
