package com.soomgil.preference.api.dto;

/**
 * 마이페이지 여행 취향 카테고리 항목.
 *
 * @param category  그룹 한국어 라벨 (예: "자연/경관")
 * @param groupCode 그룹 코드 (프론트 색상 매핑용)
 * @param percentage 0~100 선호도 퍼센트
 */
public record MyPreferenceCategory(
	String category,
	String groupCode,
	int percentage
) {
}
