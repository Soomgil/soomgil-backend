package com.soomgil.place.application.query.dto;

/**
 * 관광사진 공모전 수상작 목록 조회 query.
 *
 * @param limit 최대 반환 개수. 1 미만이면 기본값으로, 상한을 넘으면 상한으로 보정한다
 * @param regionCode 시도 코드 필터. null이면 전체 지역을 대상으로 한다
 */
public record ListAwardPhotosQuery(
	int limit,
	String regionCode
) {
}
