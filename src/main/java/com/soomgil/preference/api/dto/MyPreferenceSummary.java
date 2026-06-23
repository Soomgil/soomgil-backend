package com.soomgil.preference.api.dto;

import java.util.List;

/**
 * 마이페이지 여행 취향 분석 결과.
 *
 * @param topCategories 그룹별 선호도 상위 목록 (최대 5개)
 * @param travelStyle   취향 요약 문장
 * @param preferredTags 개별 태그 한국어 라벨 목록 (최대 8개)
 */
public record MyPreferenceSummary(
	List<MyPreferenceCategory> topCategories,
	String travelStyle,
	List<String> preferredTags
) {
}
