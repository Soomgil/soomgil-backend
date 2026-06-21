package com.soomgil.place.infrastructure.persistence.row;

import java.time.OffsetDateTime;

/**
 * 관광 원천 장소 상세 SQL 결과 row.
 *
 * @param contentId KTO content id
 * @param title 장소명
 * @param address 주소
 * @param latitude 위도
 * @param longitude 경도
 * @param thumbnailUrl 대표 이미지 URL 문자열
 * @param category 관광지 분류
 * @param overview 장소 설명
 * @param tel 전화번호
 * @param sourceModifiedAt 원천 수정 시각
 */
public record TourismSourcePlaceDetailRow(
	Integer contentId,
	String title,
	String address,
	Double latitude,
	Double longitude,
	String thumbnailUrl,
	String category,
	String overview,
	String tel,
	OffsetDateTime sourceModifiedAt
) {
}
