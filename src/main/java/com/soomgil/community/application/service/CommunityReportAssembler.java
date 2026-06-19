package com.soomgil.community.application.service;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.ReportReason;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.domain.model.ContentReportRecord;
import com.soomgil.community.domain.model.ModerationActionRecord;
import com.soomgil.community.domain.model.ReportReasonRecord;
import com.soomgil.user.api.dto.UserSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 신고/모더레이션 도메인 레코드를 API 응답 DTO로 조립한다.
 *
 * <p>신고자/모더레이터의 {@link UserSummary}를 auth 모듈의 query interface로 해결한다.
 */
@Component
public class CommunityReportAssembler {

	private final FindDisplayNameQueryHandler displayNameQueryHandler;

	public CommunityReportAssembler(FindDisplayNameQueryHandler displayNameQueryHandler) {
		this.displayNameQueryHandler = displayNameQueryHandler;
	}

	/**
	 * {@link ReportReasonRecord}를 {@link ReportReason} DTO로 변환한다.
	 *
	 * @param record 사유 레코드
	 * @return 사유 DTO
	 */
	public ReportReason toReason(ReportReasonRecord record) {
		return new ReportReason(
			ReportReasonCode.valueOf(record.code()),
			record.displayName(),
			record.isActive()
		);
	}

	/**
	 * {@link ContentReportRecord}를 {@link ContentReport} DTO로 조립한다.
	 *
	 * @param record 신고 레코드
	 * @return 신고 DTO
	 */
	public ContentReport toReport(ContentReportRecord record) {
		return new ContentReport(
			record.id(),
			resolveUser(record.reporterUserId()),
			record.targetType(),
			record.targetId(),
			record.reasonCode(),
			record.detail(),
			record.status(),
			toOffsetDateTime(record.createdAt()),
			toOffsetDateTime(record.resolvedAt()),
			record.resolutionNote()
		);
	}

	/**
	 * {@link ModerationActionRecord}를 {@link ModerationAction} DTO로 조립한다.
	 *
	 * @param record 조치 레코드
	 * @return 조치 DTO
	 */
	public ModerationAction toAction(ModerationActionRecord record) {
		return new ModerationAction(
			record.id(),
			resolveUser(record.moderatorUserId()),
			record.targetType(),
			record.targetId(),
			record.action(),
			record.moderationStatus(),
			record.moderationReason(),
			toOffsetDateTime(record.createdAt())
		);
	}

	private UserSummary resolveUser(UUID userId) {
		String displayName = displayNameQueryHandler.handle(new FindDisplayNameQuery(userId));
		return new UserSummary(userId, displayName, null);
	}

	private OffsetDateTime toOffsetDateTime(Instant instant) {
		return instant != null ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
	}
}
