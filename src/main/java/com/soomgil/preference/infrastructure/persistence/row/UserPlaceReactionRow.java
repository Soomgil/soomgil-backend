package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 현재 사용자-장소 최종 반응 row.
 *
 * @param id reaction id 문자열
 * @param reaction 최종 반응
 * @param placeTagEnrichmentId 최종 반응에 반영한 장소 태깅 실행 ID
 * @param source 마지막 반응 source
 * @param sourceResourceId 마지막 source resource 식별자
 * @param evidenceMultiplier projection에 반영된 근거 배수
 */
public record UserPlaceReactionRow(
	String id,
	String reaction,
	String placeTagEnrichmentId,
	String source,
	String sourceResourceId,
	BigDecimal evidenceMultiplier
) {

	public UserPlaceReactionRow(String id, String reaction, String placeTagEnrichmentId) {
		this(id, reaction, placeTagEnrichmentId, "HOME_BACKGROUND", null, BigDecimal.ONE);
	}
}
