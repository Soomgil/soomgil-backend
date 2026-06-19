package com.soomgil.place.infrastructure.persistence.row;

/**
 * 관광 원천 장소 이미지 후보 SQL 결과 row.
 *
 * @param imageType 이미지 후보 원천 종류
 * @param publicUrl 공개 이미지 URL 문자열
 * @param sourceType 원천 이미지 세부 타입
 * @param displayOrder 노출 순서
 * @param width 이미지 너비
 * @param height 이미지 높이
 */
public record TourismSourcePlaceImageRow(
	String imageType,
	String publicUrl,
	String sourceType,
	Integer displayOrder,
	Integer width,
	Integer height
) {
}
