package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 가입 취향 설문 장소와 화면 표시용 관광 원천 정보를 합친 저장소 읽기 모델.
 */
public record OnboardingSurveyPlaceRow(
	String provider,
	String externalPlaceId,
	String name,
	String address,
	String thumbnailUrl,
	String category,
	String description,
	String tagLabels,
	int sortOrder
) {
}
