package com.soomgil.place.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.List;

/**
 * 여행 지역을 기준으로 장소 후보를 조회하는 query.
 *
 * <p>지도 viewport에 의존하는 {@link PlaceViewportCandidateQuery}와 달리 bbox를 요구하지 않는다.
 * 투표 후보 생성처럼 사용자가 지도를 보고 있지 않은 서버-side 흐름에서 사용한다.
 *
 * <p>{@code legalRegionCodes}가 비어 있으면 {@code keyword}로 대체 검색한다. 둘 다 없으면 빈 목록을 반환한다.
 *
 * @param legalRegionCodes 법정동 코드 목록. 여러 지역을 합쳐 후보를 모은다
 * @param keyword 지역 코드가 없을 때 사용할 대체 검색어. 예: 여행방 대표 목적지
 * @param category 관광지 분류. 제한하지 않으면 null
 * @param limit 최대 후보 수
 */
public record PlaceRegionCandidateQuery(
	List<String> legalRegionCodes,
	String keyword,
	String category,
	int limit
) implements Query<List<PlaceViewportCandidate>> {

	public PlaceRegionCandidateQuery {
		legalRegionCodes = legalRegionCodes == null ? List.of() : List.copyOf(legalRegionCodes);
	}
}
