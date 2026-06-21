package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.ReportReason;
import java.util.List;

/**
 * 활성 신고 사유 목록 조회 요청.
 */
public record ListReportReasonsQuery() implements Query<List<ReportReason>> {
}
