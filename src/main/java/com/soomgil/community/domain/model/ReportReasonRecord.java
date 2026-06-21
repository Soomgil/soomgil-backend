package com.soomgil.community.domain.model;

/**
 * {@code community.report_reasons} row를 나타내는 도메인 레코드.
 *
 * <p>활성({@code isActive=true}) 신고 사유만 조회 API에 노출된다.
 * {@code sortOrder} 오름차순으로 정렬한다.
 *
 * @param code 사유 코드 (예: {@code SPAM})
 * @param displayName 표시명 (예: "스팸 · 광고")
 * @param isActive 활성 여부
 * @param sortOrder 정렬 순서
 */
public record ReportReasonRecord(
	String code,
	String displayName,
	boolean isActive,
	int sortOrder
) {
}
