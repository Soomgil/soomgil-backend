package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * TRIP_VOTE 근거를 사용자 태그 projection에 가산하기 위한 row.
 *
 * <p>{@code evidence}는 positive_evidence와 vote_evidence 양쪽에 더해진다.
 * positive_evidence에 함께 더하는 이유는 기존 추천 점수 계산 공식을 바꾸지 않기 위해서다.
 *
 * @param userId 사용자 식별자
 * @param tagId 태그 식별자
 * @param evidence 가산할 근거
 */
public record UserTagVoteEvidenceRow(
	String userId,
	String tagId,
	BigDecimal evidence
) {
}
