package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ReportStatus;
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
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고를 처리(RESOLVED 또는 REJECTED)한다.
 *
 * <p>모더레이터 권한을 먼저 검증한다. 이미 {@code RESOLVED} 또는 {@code REJECTED}인 신고는
 * 재처리할 수 없다({@link ErrorCode#INVALID_REPORT_TRANSITION}).
 * {@code actionType}이 null이 아니면 신고 대상에 모더레이션 조치를 함께 적용하고
 * 조치 이력을 기록한다.
 */
@Component
@Transactional
public class ResolveReportCommandHandler
	implements CommandHandler<ResolveReportCommand, ContentReport> {

	private final ContentReportMapper reportMapper;
	private final ModerationAccessGuard accessGuard;
	private final ModerationTargetService targetService;
	private final ModerationActionMapper actionMapper;
	private final CommunityReportAssembler assembler;

	public ResolveReportCommandHandler(
		ContentReportMapper reportMapper,
		ModerationAccessGuard accessGuard,
		ModerationTargetService targetService,
		ModerationActionMapper actionMapper,
		CommunityReportAssembler assembler
	) {
		this.reportMapper = reportMapper;
		this.accessGuard = accessGuard;
		this.targetService = targetService;
		this.actionMapper = actionMapper;
		this.assembler = assembler;
	}

	@Override
	public ContentReport handle(ResolveReportCommand command) {
		accessGuard.requireModerator(command.moderatorUserId());

		ContentReportRecord report = reportMapper.findById(command.reportId())
			.orElseThrow(() -> new CommunityException(ErrorCode.REPORT_NOT_FOUND));

		if (report.status() == ReportStatus.RESOLVED || report.status() == ReportStatus.REJECTED) {
			throw new CommunityException(ErrorCode.INVALID_REPORT_TRANSITION);
		}

		Instant now = Instant.now();

		if (command.actionType() != null) {
			var resultingStatus = targetService.applyAction(
				report.targetType(), report.targetId(),
				command.actionType(), command.actionReason(), now);

			actionMapper.insert(
				UUID.randomUUID(),
				command.moderatorUserId(),
				report.targetType(),
				report.targetId(),
				command.actionType(),
				resultingStatus,
				command.actionReason(),
				report.id(),
				now
			);
		}

		reportMapper.updateResolution(
			report.id(),
			command.status(),
			command.resolutionNote(),
			command.moderatorUserId(),
			now
		);

		return assembler.toReport(reportMapper.findById(command.reportId()).orElseThrow());
	}
}
