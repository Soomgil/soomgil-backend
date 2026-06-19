package com.soomgil.community.domain.model;

import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code community.moderation_actions} row를 나타내는 도메인 레코드.
 *
 * <p>모더레이터가 수행한 조치(HIDE, RESTORE, DELETE)와 그 결과로 target에 적용된
 * moderation status를 기록한다. {@code reportId}는 신고 해결 시 파생된 조치인 경우
 * 해당 신고를 가리킨다.
 *
 * @param id 조치 식별자
 * @param moderatorUserId 모더레이터
 * @param targetType 조치 대상 유형
 * @param targetId 조치 대상 식별자
 * @param action 조치 유형
 * @param moderationStatus 결과 moderation status (nullable)
 * @param moderationReason 조치 사유 (nullable)
 * @param reportId 연관 신고 (nullable)
 * @param createdAt 생성 시각
 */
public record ModerationActionRecord(
	UUID id,
	UUID moderatorUserId,
	ReportTargetType targetType,
	UUID targetId,
	ModerationActionType action,
	ModerationStatus moderationStatus,
	String moderationReason,
	UUID reportId,
	Instant createdAt
) {
}
