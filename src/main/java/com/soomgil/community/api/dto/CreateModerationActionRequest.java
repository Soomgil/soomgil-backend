package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 모더레이션 조치 생성 요청.
 *
 * <p>모더레이터가 신고된 콘텐츠에 대해 숨김/복원/삭제 조치를 적용할 때 사용한다.
 * 조치는 audit 로그로 남으며, 대상 콘텐츠의 {@code ModerationStatus}를 함께 변경한다.
 *
 * @param targetType 조치 대상 종류 (POST, POST_COMMENT)
 * @param targetId 조치 대상 식별자
 * @param action 조치 유형 (HIDE, RESTORE, DELETE)
 * @param moderationReason 조치 사유 메모
 */
public record CreateModerationActionRequest(
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	@NotNull
	ModerationActionType action,
	String moderationReason
) {
}
