package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 현재 사용자-장소 최종 반응 row.
 *
 * @param id reaction id 문자열
 * @param reaction 최종 반응
 * @param placeTagEnrichmentId 최종 반응에 반영한 장소 태깅 실행 ID
 */
public record UserPlaceReactionRow(
	String id,
	String reaction,
	String placeTagEnrichmentId
) {
}
