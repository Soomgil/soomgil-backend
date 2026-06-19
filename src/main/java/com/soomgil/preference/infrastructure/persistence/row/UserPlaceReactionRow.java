package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 현재 사용자-장소 최종 반응 row.
 *
 * @param id reaction id 문자열
 * @param reaction 최종 반응
 */
public record UserPlaceReactionRow(
	String id,
	String reaction
) {
}
