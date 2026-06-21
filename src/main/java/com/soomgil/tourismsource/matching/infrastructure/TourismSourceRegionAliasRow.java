package com.soomgil.tourismsource.matching.infrastructure;

/**
 * 관광 원천 지역 alias row.
 *
 * @param normalizedAlias 비교용 정규화 alias
 * @param sidoCode 시도 코드
 * @param gugunCode 구군 코드
 */
public record TourismSourceRegionAliasRow(
	String normalizedAlias,
	Integer sidoCode,
	Integer gugunCode
) {
}
