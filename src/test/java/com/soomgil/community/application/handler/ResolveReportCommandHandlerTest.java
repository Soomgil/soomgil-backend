package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;

import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.application.command.ResolveReportCommand;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.application.service.ModerationAccessGuard;
import com.soomgil.community.application.service.ModerationTargetService;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.ContentReportRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ContentReportMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ModerationActionMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResolveReportCommandHandlerTest {

	private final ContentReportMapper reportMapper = mock(ContentReportMapper.class);
	private final ModerationAccessGuard accessGuard = mock(ModerationAccessGuard.class);
	private final ModerationTargetService targetService = mock(ModerationTargetService.class);
	private final ModerationActionMapper actionMapper = mock(ModerationActionMapper.class);
	private final CommunityReportAssembler assembler = mock(CommunityReportAssembler.class);

	private final ResolveReportCommandHandler handler =
		new ResolveReportCommandHandler(reportMapper, accessGuard, targetService, actionMapper, assembler);

	@Test
	@DisplayName("OPEN → RESOLVED 정상 처리 시 report 상태가 업데이트된다")
	void resolvesOpenReport() {
		UUID reportId = UUID.randomUUID();
		UUID moderatorId = UUID.randomUUID();

		when(reportMapper.findById(reportId))
			.thenReturn(Optional.of(sampleReport(reportId, ReportStatus.OPEN)))
			.thenReturn(Optional.of(sampleReport(reportId, ReportStatus.RESOLVED)));
		doNothing().when(accessGuard).requireModerator(moderatorId);
		when(assembler.toReport(any())).thenReturn(new ContentReport(
			reportId, null, ReportTargetType.POST, UUID.randomUUID(),
			ReportReasonCode.SPAM, "detail", ReportStatus.RESOLVED,
			OffsetDateTime.now(), OffsetDateTime.now(), "resolved"
		));

		ContentReport result = handler.handle(new ResolveReportCommand(
			reportId, moderatorId, ReportStatus.RESOLVED, "resolved", null, null
		));

		assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
		verify(reportMapper).updateResolution(
			eq(reportId), eq(ReportStatus.RESOLVED), eq("resolved"),
			eq(moderatorId), any(Instant.class)
		);
	}

	@Test
	@DisplayName("이미 RESOLVED인 신고 재처리 시 INVALID_REPORT_TRANSITION을 반환한다")
	void rejectsTransitionFromResolved() {
		UUID reportId = UUID.randomUUID();
		UUID moderatorId = UUID.randomUUID();

		when(reportMapper.findById(reportId))
			.thenReturn(Optional.of(sampleReport(reportId, ReportStatus.RESOLVED)));
		doNothing().when(accessGuard).requireModerator(moderatorId);

		assertThatThrownBy(() -> handler.handle(new ResolveReportCommand(
			reportId, moderatorId, ReportStatus.RESOLVED, "again", null, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.INVALID_REPORT_TRANSITION));
	}

	@Test
	@DisplayName("actionType 포함 시 모더레이션 조치가 대상에 적용된다")
	void appliesModerationActionWhenRequested() {
		UUID reportId = UUID.randomUUID();
		UUID moderatorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		ContentReportRecord openReport = new ContentReportRecord(
			reportId, UUID.randomUUID(), ReportTargetType.POST, targetId,
			ReportReasonCode.SPAM, "spam", ReportStatus.OPEN,
			null, null, null, Instant.now()
		);

		when(reportMapper.findById(reportId))
			.thenReturn(Optional.of(openReport))
			.thenReturn(Optional.of(openReport));
		doNothing().when(accessGuard).requireModerator(moderatorId);
		when(targetService.applyAction(
			eq(ReportTargetType.POST), eq(targetId), eq(ModerationActionType.HIDE), any(), any(Instant.class)))
			.thenReturn(ModerationStatus.HIDDEN);
		when(assembler.toReport(any())).thenReturn(new ContentReport(
			reportId, null, ReportTargetType.POST, targetId,
			ReportReasonCode.SPAM, "spam", ReportStatus.RESOLVED,
			OffsetDateTime.now(), OffsetDateTime.now(), "hidden"
		));

		handler.handle(new ResolveReportCommand(
			reportId, moderatorId, ReportStatus.RESOLVED, "hidden",
			ModerationActionType.HIDE, "spam content"
		));

		verify(targetService).applyAction(
			eq(ReportTargetType.POST), eq(targetId), eq(ModerationActionType.HIDE),
			eq("spam content"), any(Instant.class)
		);
		verify(actionMapper).insert(
			any(UUID.class), eq(moderatorId), eq(ReportTargetType.POST), eq(targetId),
			eq(ModerationActionType.HIDE), eq(ModerationStatus.HIDDEN),
			eq("spam content"), eq(reportId), any(Instant.class)
		);
	}

	private static ContentReportRecord sampleReport(UUID id, ReportStatus status) {
		return new ContentReportRecord(
			id, UUID.randomUUID(), ReportTargetType.POST, UUID.randomUUID(),
			ReportReasonCode.SPAM, "detail", status, null, null, null, Instant.now()
		);
	}
}
