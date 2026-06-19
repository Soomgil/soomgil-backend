package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportTargetType;
import java.util.UUID;

/**
 * 콘텐츠 신고 생성 요청.
 *
 * <p>호출자는 인증된 사용자이며, 본인이 작성한 콘텐츠는 신고할 수 없다.
 *
 * @param reporterUserId 신고자
 * @param targetType 신고 대상 유형 (POST 또는 POST_COMMENT)
 * @param targetId 신고 대상 식별자
 * @param reasonCode 신고 사유
 * @param detail 상세 설명 (nullable, 최대 2000자)
 */
public record CreateContentReportCommand(
	UUID reporterUserId,
	ReportTargetType targetType,
	UUID targetId,
	ReportReasonCode reasonCode,
	String detail
) implements Command<ContentReport> {
}
