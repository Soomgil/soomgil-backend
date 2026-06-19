package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.application.command.CreateContentReportCommand;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.application.service.ModerationTargetService;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.infrastructure.persistence.mapper.ContentReportMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateContentReportCommandHandlerTest {

	private final ContentReportMapper reportMapper = mock(ContentReportMapper.class);
	private final ModerationTargetService targetService = mock(ModerationTargetService.class);
	private final CommunityReportAssembler assembler = mock(CommunityReportAssembler.class);

	private final CreateContentReportCommandHandler handler =
		new CreateContentReportCommandHandler(reportMapper, targetService, assembler);

	@Test
	@DisplayName("게시글 신고 - 정상 처리 시 status=OPEN으로 INSERT된다")
	void createsReportForPost() {
		UUID reporterId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		when(targetService.requireTargetAndReturnOwner(ReportTargetType.POST, targetId))
			.thenReturn(ownerId);
		when(reportMapper.existsOpenByReporterAndTarget(reporterId, ReportTargetType.POST, targetId))
			.thenReturn(false);
		when(assembler.toReport(any())).thenReturn(new ContentReport(
			UUID.randomUUID(), null, ReportTargetType.POST, targetId,
			ReportReasonCode.SPAM, "spam post", ReportStatus.OPEN,
			OffsetDateTime.now(), null, null
		));

		ContentReport result = handler.handle(new CreateContentReportCommand(
			reporterId, ReportTargetType.POST, targetId, ReportReasonCode.SPAM, "spam post"
		));

		assertThat(result).isNotNull();
		assertThat(result.status()).isEqualTo(ReportStatus.OPEN);
		verify(reportMapper).insert(
			any(UUID.class), eq(reporterId), eq(ReportTargetType.POST), eq(targetId),
			eq("SPAM"), eq("spam post"), any(Instant.class)
		);
	}

	@Test
	@DisplayName("댓글 신고 - target_type=POST_COMMENT로 INSERT된다")
	void createsReportForComment() {
		UUID reporterId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		when(targetService.requireTargetAndReturnOwner(ReportTargetType.POST_COMMENT, targetId))
			.thenReturn(ownerId);
		when(reportMapper.existsOpenByReporterAndTarget(reporterId, ReportTargetType.POST_COMMENT, targetId))
			.thenReturn(false);
		when(assembler.toReport(any())).thenReturn(new ContentReport(
			UUID.randomUUID(), null, ReportTargetType.POST_COMMENT, targetId,
			ReportReasonCode.HARASSMENT_OR_HATE, "bad comment", ReportStatus.OPEN,
			OffsetDateTime.now(), null, null
		));

		handler.handle(new CreateContentReportCommand(
			reporterId, ReportTargetType.POST_COMMENT, targetId,
			ReportReasonCode.HARASSMENT_OR_HATE, "bad comment"
		));

		verify(reportMapper).insert(
			any(UUID.class), eq(reporterId), eq(ReportTargetType.POST_COMMENT), eq(targetId),
			eq("HARASSMENT_OR_HATE"), eq("bad comment"), any(Instant.class)
		);
	}

	@Test
	@DisplayName("본인 콘텐츠 신고 시 CANNOT_REPORT_OWN_CONTENT를 반환한다")
	void rejectsReportingOwnContent() {
		UUID reporterId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		when(targetService.requireTargetAndReturnOwner(ReportTargetType.POST, targetId))
			.thenReturn(reporterId);

		assertThatThrownBy(() -> handler.handle(new CreateContentReportCommand(
			reporterId, ReportTargetType.POST, targetId, ReportReasonCode.SPAM, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.CANNOT_REPORT_OWN_CONTENT));
	}

	@Test
	@DisplayName("동일 대상 중복 신고 시 DUPLICATE_REPORT를 반환한다")
	void rejectsDuplicateReport() {
		UUID reporterId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		when(targetService.requireTargetAndReturnOwner(ReportTargetType.POST, targetId))
			.thenReturn(ownerId);
		when(reportMapper.existsOpenByReporterAndTarget(reporterId, ReportTargetType.POST, targetId))
			.thenReturn(true);

		assertThatThrownBy(() -> handler.handle(new CreateContentReportCommand(
			reporterId, ReportTargetType.POST, targetId, ReportReasonCode.SPAM, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.DUPLICATE_REPORT));
	}
}
