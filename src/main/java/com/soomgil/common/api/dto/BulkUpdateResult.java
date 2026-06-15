package com.soomgil.common.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 여러 row나 resource를 한 번에 변경한 command의 공통 결과.
 *
 * <p>{@code updatedCount}는 실제로 변경된 대상 개수를 의미하며, 요청 개수와 다를 수 있다.
 */
public record BulkUpdateResult(
	@NotNull
	Integer updatedCount
) {
}
