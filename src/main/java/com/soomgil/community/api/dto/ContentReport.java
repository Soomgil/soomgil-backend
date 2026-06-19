package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 콘텐츠 신고 응답.
 *
 * <p>사용자가 게시글 또는 댓글에 대해 제출한 신고 내역을 나타낸다.
 * {@code status}가 OPEN 또는 REVIEWING인 경우 아직 처리되지 않은 신고다.
 * 신고자가 탈퇴한 경우 {@code reporter}는 null일 수 있다.
 *
 * @param id 신고 식별자
 * @param reporter 신고자 요약 정보, 탈퇴한 경우 null
 * @param targetType 신고 대상 종류 (POST, POST_COMMENT)
 * @param targetId 신고 대상 식별자
 * @param reasonCode 신고 사유 코드
 * @param detail 신고 상세 설명
 * @param status 신고 처리 상태
 * @param createdAt 신고 생성 시각
 * @param resolvedAt 신고 해결 시각, 미해결이면 null
 * @param resolutionNote 모더레이터가 남긴 해결 메모
 */
public record ContentReport(
	@NotNull
	UUID id,
	@Valid
	UserSummary reporter,
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	ReportReasonCode reasonCode,
	String detail,
	@NotNull
	ReportStatus status,
	@NotNull
	OffsetDateTime createdAt,
	OffsetDateTime resolvedAt,
	String resolutionNote
) {
}
