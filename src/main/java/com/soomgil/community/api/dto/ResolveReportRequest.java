package com.soomgil.community.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 신고 해결 요청.
 *
 * <p>모더레이터가 신고를 처리할 때 사용한다.
 * {@code status}는 RESOLVED 또는 REJECTED여야 하며,
 * 모더레이션 조치가 필요한 경우 {@code moderationAction}에 조치 요청을 함께 전달한다.
 *
 * @param status 처리 상태 (RESOLVED, REJECTED)
 * @param resolutionNote 해결 메모
 * @param moderationAction 동반 적용할 모더레이션 조치 요청, 생략 가능
 */
public record ResolveReportRequest(
	@NotBlank
	String status,
	String resolutionNote,
	@Valid
	CreateModerationActionRequest moderationAction
) {
}
