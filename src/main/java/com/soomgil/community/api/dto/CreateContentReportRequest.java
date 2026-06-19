package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 콘텐츠 신고 생성 요청.
 *
 * <p>게시글 또는 댓글에 대한 신고를 생성한다.
 * 동일 대상에 대한 중복 신고는 허용할 수 있으나, 운영 정책에 따라 제한될 수 있다.
 *
 * @param targetType 신고 대상 종류 (POST, POST_COMMENT)
 * @param targetId 신고 대상 식별자
 * @param reasonCode 신고 사유 코드
 * @param detail 신고 상세 설명 (최대 2000자)
 */
public record CreateContentReportRequest(
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	@NotNull
	ReportReasonCode reasonCode,
	@Size(max = 2000)
	String detail
) {
}
