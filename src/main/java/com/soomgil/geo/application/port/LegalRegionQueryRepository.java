package com.soomgil.geo.application.port;

import com.soomgil.geo.domain.model.LegalRegionLevel;

/**
 * 법정동 지역 읽기 persistence 계약.
 */
public interface LegalRegionQueryRepository {

	/**
	 * 법정동 지역 목록을 조회한다.
	 *
	 * @param query 이름 또는 전체 이름 검색어
	 * @param level 선택적 지역 level
	 * @param parentCode 선택적 상위 지역 code
	 * @param isActive 선택적 활성 상태
	 * @param page 0 기반 page
	 * @param size page 크기
	 * @return page 목록과 전체 개수
	 */
	LegalRegionPage findLegalRegions(
		String query,
		LegalRegionLevel level,
		String parentCode,
		Boolean isActive,
		int page,
		int size
	);
}
