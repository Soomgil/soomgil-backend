package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.application.command.CreateContentReportCommand;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.application.service.ModerationTargetService;
import com.soomgil.community.domain.model.ContentReportRecord;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.infrastructure.persistence.mapper.ContentReportMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 신고를 생성한다.
 *
 * <p>검증 순서:
 * <ol>
 *   <li>대상이 존재하고 삭제되지 않았는지 확인 ({@link ErrorCode#REPORT_TARGET_NOT_FOUND})</li>
 *   <li>본인 콘텐츠 신고 차단 ({@link ErrorCode#CANNOT_REPORT_OWN_CONTENT})</li>
 *   <li>동일 대상 중복 신고 차단 ({@link ErrorCode#DUPLICATE_REPORT})</li>
 * </ol>
 * 모든 검증을 통과하면 {@code status=OPEN}으로 INSERT한다.
 */
@Component
@Transactional
public class CreateContentReportCommandHandler
	implements CommandHandler<CreateContentReportCommand, ContentReport> {

	private final ContentReportMapper reportMapper;
	private final ModerationTargetService targetService;
	private final CommunityReportAssembler assembler;

	public CreateContentReportCommandHandler(
		ContentReportMapper reportMapper,
		ModerationTargetService targetService,
		CommunityReportAssembler assembler
	) {
		this.reportMapper = reportMapper;
		this.targetService = targetService;
		this.assembler = assembler;
	}

	@Override
	public ContentReport handle(CreateContentReportCommand command) {
		UUID ownerUserId = targetService.requireTargetAndReturnOwner(
			command.targetType(), command.targetId());

		if (ownerUserId.equals(command.reporterUserId())) {
			throw new CommunityException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
		}

		if (reportMapper.existsOpenByReporterAndTarget(
			command.reporterUserId(), command.targetType(), command.targetId())) {
			throw new CommunityException(ErrorCode.DUPLICATE_REPORT);
		}

		UUID reportId = UUID.randomUUID();
		Instant now = Instant.now();

		reportMapper.insert(
			reportId,
			command.reporterUserId(),
			command.targetType(),
			command.targetId(),
			command.reasonCode().name(),
			command.detail(),
			now
		);

		ContentReportRecord record = new ContentReportRecord(
			reportId,
			command.reporterUserId(),
			command.targetType(),
			command.targetId(),
			command.reasonCode(),
			command.detail(),
			ReportStatus.OPEN,
			null,
			null,
			null,
			now
		);

		return assembler.toReport(record);
	}
}
