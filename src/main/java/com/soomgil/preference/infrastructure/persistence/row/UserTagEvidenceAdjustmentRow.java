package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 사용자 태그 근거와 현재 반응 횟수 조정 row.
 */
public record UserTagEvidenceAdjustmentRow(
	String userId,
	String tagId,
	BigDecimal evidence,
	String reaction
) {
}
