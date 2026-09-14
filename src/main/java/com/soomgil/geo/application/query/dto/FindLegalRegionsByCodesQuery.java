package com.soomgil.geo.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.List;

/**
 * 법정동 코드 목록으로 지역을 한 번에 조회하는 query.
 *
 * <p>여행방 지역 코드처럼 코드만 들고 있는 호출자가 이름과 level을 알아야 할 때 사용한다. 검색어 기반
 * {@link ListLegalRegionsQuery}와 달리 페이지가 없고, 요청한 코드 중 존재하는 지역만 돌려준다.
 *
 * @param codes 10자리 법정동 코드 목록. 비어 있으면 저장소를 조회하지 않는다
 */
public record FindLegalRegionsByCodesQuery(List<String> codes) implements Query<List<LegalRegionView>> {
	public FindLegalRegionsByCodesQuery {
		codes = codes == null ? List.of() : List.copyOf(codes);
	}
}
