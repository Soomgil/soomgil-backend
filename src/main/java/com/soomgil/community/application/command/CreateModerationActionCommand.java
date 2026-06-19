package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ReportTargetType;
import java.util.UUID;

/**
 * 모더레이션 조치 생성 요청.
 *
 * <p>모더레이터가 신고와 무관하게 직접 조치(HIDE/RESTORE/DELETE)를 적용할 때 사용한다.
 * {@code reportId}는 신고 해결에서 파생된 경우에만 설정한다.
 *
 * @param moderatorUserId 모더레이터
 * @param targetType 조치 대상 유형
 * @param targetId 조치 대상 식별자
 * @param action 조치 유형
 * @param moderationReason 조치 사유 (nullable)
 * @param reportId 연관 신고 (nullable)
 */
public record CreateModerationActionCommand(
	UUID moderatorUserId,
	ReportTargetType targetType,
	UUID targetId,
	ModerationActionType action,
	String moderationReason,
	UUID reportId
) implements Command<ModerationAction> {
}
