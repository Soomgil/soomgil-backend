package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.ReportReason;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.application.query.ListReportReasonsQuery;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.domain.model.ReportReasonRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ReportReasonMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListReportReasonsQueryHandlerTest {

	private final ReportReasonMapper reasonMapper = mock(ReportReasonMapper.class);
	private final CommunityReportAssembler assembler = mock(CommunityReportAssembler.class);

	private final ListReportReasonsQueryHandler handler =
		new ListReportReasonsQueryHandler(reasonMapper, assembler);

	@Test
	@DisplayName("활성 신고 사유를 sort_order 순으로 반환한다")
	void returnsActiveReasonsInSortOrder() {
		when(reasonMapper.findAllActive()).thenReturn(List.of(
			new ReportReasonRecord("SPAM", "스팸 · 광고", true, 1),
			new ReportReasonRecord("OTHER", "기타", true, 5)
		));
		when(assembler.toReason(reasonMapper.findAllActive().get(0)))
			.thenReturn(new ReportReason(ReportReasonCode.SPAM, "스팸 · 광고", true));

		List<ReportReason> result = handler.handle(new ListReportReasonsQuery());

		assertThat(result).hasSize(2);
	}

	@Test
	@DisplayName("활성 신고 사유가 없으면 빈 목록을 반환한다")
	void returnsEmptyListWhenNoActiveReasons() {
		when(reasonMapper.findAllActive()).thenReturn(List.of());

		List<ReportReason> result = handler.handle(new ListReportReasonsQuery());

		assertThat(result).isEmpty();
	}
}
