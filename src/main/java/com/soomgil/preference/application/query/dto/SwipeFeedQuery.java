package com.soomgil.preference.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.preference.api.dto.SwipeFeedResponse;

/**
 * 전역 스와이프 feed를 조회하는 query.
 *
 * @param legalRegionCode 지역 코드
 * @param category 관광지 분류
 * @param limit 최대 feed 항목 수
 * @param excludeRecent 이미 반응한 장소 제외 여부
 * @param seed feed continuation seed
 */
public record SwipeFeedQuery(
	String legalRegionCode,
	String category,
	int limit,
	boolean excludeRecent,
	String seed
) implements Query<SwipeFeedResponse> {
}
