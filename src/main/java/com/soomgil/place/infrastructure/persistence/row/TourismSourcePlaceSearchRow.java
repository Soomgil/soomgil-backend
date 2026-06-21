package com.soomgil.place.infrastructure.persistence.row;

/**
 * 관광 원천 장소 검색 SQL 결과 row.
 *
 * @param contentId KTO content id
 * @param title 장소명
 * @param address 주소
 * @param latitude 위도
 * @param longitude 경도
 * @param thumbnailUrl 대표 이미지 URL 문자열
 * @param category 관광지 분류
 */
public record TourismSourcePlaceSearchRow(
	Integer contentId,
	String title,
	String address,
	Double latitude,
	Double longitude,
	String thumbnailUrl,
	String category
) {
}
