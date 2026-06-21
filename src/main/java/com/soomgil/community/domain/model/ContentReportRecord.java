package com.soomgil.community.domain.model;

import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code community.content_reports} row를 나타내는 도메인 레코드.
 *
 * <p>신고 생성 시 {@code status}는 {@code OPEN}이며, 모더레이터가 검토 후
 * {@code RESOLVED} 또는 {@code REJECTED}로 전환한다.
 *
 * @param id 신고 식별자
 * @param reporterUserId 신고자
 * @param targetType 신고 대상 유형 (POST 또는 POST_COMMENT)
 * @param targetId 신고 대상 식별자
 * @param reasonCode 신고 사유 코드
 * @param detail 상세 설명 (nullable, 최대 2000자)
 * @param status 신고 상태
 * @param resolutionNote 모더레이터 처리 메모 (nullable)
 * @param resolvedBy 처리자 (nullable)
 * @param resolvedAt 처리 시각 (nullable)
 * @param createdAt 생성 시각
 */
public record ContentReportRecord(
	UUID id,
	UUID reporterUserId,
	ReportTargetType targetType,
	UUID targetId,
	ReportReasonCode reasonCode,
	String detail,
	ReportStatus status,
	String resolutionNote,
	UUID resolvedBy,
	Instant resolvedAt,
	Instant createdAt
) {
}
