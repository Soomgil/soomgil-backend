package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;

/**
 * 장소 태그 후보의 서버 선택 결과.
 */
public record PlaceTagSelectionDecision(
	PlaceTagSelectionInput input,
	BigDecimal selectionScore,
	String status
) {

	public boolean selected() {
		return "SELECTED".equals(status);
	}
}
