package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.ReportReason;
import com.soomgil.community.application.query.ListReportReasonsQuery;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.infrastructure.persistence.mapper.ReportReasonMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활성 신고 사유 목록을 조회한다.
 *
 * <p>{@code sort_order} 오름차순으로 정렬된 활성({@code is_active=true}) 사유만 반환한다.
 */
@Component
@Transactional(readOnly = true)
public class ListReportReasonsQueryHandler
	implements QueryHandler<ListReportReasonsQuery, List<ReportReason>> {

	private final ReportReasonMapper reasonMapper;
	private final CommunityReportAssembler assembler;

	public ListReportReasonsQueryHandler(
		ReportReasonMapper reasonMapper,
		CommunityReportAssembler assembler
	) {
		this.reasonMapper = reasonMapper;
		this.assembler = assembler;
	}

	@Override
	public List<ReportReason> handle(ListReportReasonsQuery query) {
		return reasonMapper.findAllActive().stream()
			.map(assembler::toReason)
			.toList();
	}
}
