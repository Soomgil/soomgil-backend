package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.PagedContentReport;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.application.query.ListReportsQuery;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.domain.model.ContentReportRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ContentReportMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListReportsQueryHandlerTest {

	private final ContentReportMapper reportMapper = mock(ContentReportMapper.class);
	private final CommunityReportAssembler assembler = mock(CommunityReportAssembler.class);

	private final ListReportsQueryHandler handler =
		new ListReportsQueryHandler(reportMapper, assembler);

	@Test
	@DisplayName("status=OPEN 필터로 신고 목록을 페이지네이션한다")
	void listsReportsByStatus() {
		ContentReportRecord record = sampleReport(ReportStatus.OPEN);

		when(reportMapper.findByStatus(ReportStatus.OPEN, 0, 20))
			.thenReturn(List.of(record));
		when(reportMapper.countByStatus(ReportStatus.OPEN)).thenReturn(1);
		when(assembler.toReport(record)).thenReturn(new ContentReport(
			record.id(), null, ReportTargetType.POST, UUID.randomUUID(),
			ReportReasonCode.SPAM, "spam", ReportStatus.OPEN,
			OffsetDateTime.now(), null, null
		));

		PagedContentReport result = handler.handle(new ListReportsQuery(ReportStatus.OPEN, 0, 20));

		assertThat(result.items()).hasSize(1);
		assertThat(result.page().totalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("status=null이면 전체 신고를 반환한다")
	void listsAllReportsWhenStatusIsNull() {
		when(reportMapper.findByStatus(null, 0, 20)).thenReturn(List.of());
		when(reportMapper.countByStatus(null)).thenReturn(0);

		PagedContentReport result = handler.handle(new ListReportsQuery(null, 0, 20));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().totalElements()).isEqualTo(0);
	}

	private ContentReportRecord sampleReport(ReportStatus status) {
		return new ContentReportRecord(
			UUID.randomUUID(), UUID.randomUUID(), ReportTargetType.POST, UUID.randomUUID(),
			ReportReasonCode.SPAM, "detail", status, null, null, null, Instant.now()
		);
	}
}
