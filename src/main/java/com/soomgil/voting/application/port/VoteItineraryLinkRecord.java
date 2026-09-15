package com.soomgil.voting.application.port;

import java.util.UUID;

/**
 * 선정 결과를 일정에 반영한 기록.
 *
 * <p>(세션, 후보) 조합이 PK이므로 이 record의 존재 자체가 "이미 처리했다"는 멱등성 근거다.
 *
 * @param voteSessionId 세션 식별자
 * @param candidateId 후보 식별자
 * @param itineraryItemId 생성된 일정 item 식별자. 중복이라 추가하지 않았으면 null
 * @param outcome {@code ADDED} 또는 {@code SKIPPED_DUPLICATE}
 */
public record VoteItineraryLinkRecord(
	UUID voteSessionId,
	UUID candidateId,
	UUID itineraryItemId,
	String outcome
) {
}
