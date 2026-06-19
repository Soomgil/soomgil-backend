package com.soomgil.preference.application.command.dto;

import java.math.BigDecimal;

/**
 * 장소 태깅 실행에서 나온 후보 태그 하나.
 *
 * @param candidateCode 모델 또는 태깅기가 제안한 태그 code
 * @param confidence 해당 태그가 장소에 맞는 정도
 * @param weight 장소 설명에서 해당 태그가 차지하는 중요도
 * @param selected upstream 선택 정책이 확정 후보로 판단했는지 여부
 * @param rationale 후보 판단 근거
 */
public record SavePlaceTagCandidateCommand(
	String candidateCode,
	BigDecimal confidence,
	BigDecimal weight,
	boolean selected,
	String rationale
) {
}
