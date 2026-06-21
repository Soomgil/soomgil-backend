package com.soomgil.community.api.dto;

/**
 * 신고 처리 상태.
 *
 * <p>OPEN이 신고 접수, REVIEWING이 모더레이터 검토 중, RESOLVED가 조치 완료,
 * REJECTED가 신고 기각을 나타낸다.
 */
public enum ReportStatus {
	OPEN,
	REVIEWING,
	RESOLVED,
	REJECTED
}
