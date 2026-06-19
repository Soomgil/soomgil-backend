package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ReportStatus;
import java.util.UUID;

/**
 * 신고 처리 요청.
 *
 * <p>모더레이터가 신고를 {@code RESOLVED} 또는 {@code REJECTED}로 전환한다.
 * {@code actionType}이 null이 아니면, 신고 대상에 대한 모더레이션 조치(HIDE/RESTORE/DELETE)를
 * 함께 수행한다.
 *
 * @param reportId 신고 식별자
 * @param moderatorUserId 모더레이터
 * @param status 전환할 상태
 * @param resolutionNote 처리 메모 (nullable)
 * @param actionType 동반 조치 유형 (nullable — 조치 없이 처리만 하는 경우 null)
 * @param actionReason 조치 사유 (nullable)
 */
public record ResolveReportCommand(
	UUID reportId,
	UUID moderatorUserId,
	ReportStatus status,
	String resolutionNote,
	ModerationActionType actionType,
	String actionReason
) implements Command<ContentReport> {
}
