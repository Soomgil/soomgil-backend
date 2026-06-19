package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.PagedContentReport;
import com.soomgil.community.api.dto.ReportStatus;

/**
 * 신고 목록 조회 요청 (모더레이터 전용).
 *
 * @param status 필터링할 상태 (null이면 전체)
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기
 */
public record ListReportsQuery(
	ReportStatus status,
	int page,
	int size
) implements Query<PagedContentReport> {
}
