package com.soomgil.preference.infrastructure.persistence.row;

/** 가입 취향 설문에 고정된 외부 관광지 참조와 표시 순서. */
public record OnboardingSurveyPlaceRefRow(
	String provider,
	String externalPlaceId,
	int sortOrder
) {
}
