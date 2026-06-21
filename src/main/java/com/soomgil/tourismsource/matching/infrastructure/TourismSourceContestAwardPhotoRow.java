package com.soomgil.tourismsource.matching.infrastructure;

/**
 * 매칭 대상 공모전 수상작 사진 row.
 *
 * @param title 사진 제목
 * @param originalFileName 원본 파일명
 * @param extractedRegionText import 시 추출한 지역 텍스트
 */
public record TourismSourceContestAwardPhotoRow(
	String title,
	String originalFileName,
	String extractedRegionText
) {
}
