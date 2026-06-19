package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 모더레이션 조치 이력 응답.
 *
 * <p>모더레이터가 콘텐츠에 가한 조치의 audit 기록을 나타낸다.
 * {@code action}이 RESTORE인 경우 이전 숨김/삭제 조치를 되돌린다.
 *
 * @param id 조치 식별자
 * @param moderator 조치를 수행한 모더레이터 요약 정보
 * @param targetType 조치 대상 종류 (POST, POST_COMMENT)
 * @param targetId 조치 대상 식별자
 * @param action 조치 유형 (HIDE, RESTORE, DELETE)
 * @param moderationStatus 조치 후 콘텐츠의 모더레이션 상태
 * @param moderationReason 조치 사유 메모
 * @param createdAt 조치 시각
 */
public record ModerationAction(
	@NotNull
	UUID id,
	@Valid
	UserSummary moderator,
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	@NotNull
	ModerationActionType action,
	ModerationStatus moderationStatus,
	String moderationReason,
	@NotNull
	OffsetDateTime createdAt
) {
}
