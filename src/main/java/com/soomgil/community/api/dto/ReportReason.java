package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 신고 사유 카탈로그 응답.
 *
 * <p>신고 화면에서 선택 가능한 사유 목록을 구성할 때 사용한다.
 * {@code isActive}가 false인 항목은 신고 UI에서 숨겨져야 한다.
 *
 * @param code 신고 사유 코드
 * @param displayName 신고 화면 표시명
 * @param isActive 활성화 여부 (false면 신고 화면에서 숨김)
 */
public record ReportReason(
	@NotNull
	ReportReasonCode code,
	@NotBlank
	String displayName,
	Boolean isActive
) {
}
