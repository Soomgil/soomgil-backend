package com.soomgil.voting.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import java.util.UUID;

/**
 * 종료된 투표의 결과를 조회하는 query. active member에게만 노출한다.
 *
 * @param tripId 여행방 식별자
 * @param sessionId 세션 식별자
 * @param userId 요청 사용자 식별자
 */
public record FindVoteSessionResultQuery(
	UUID tripId,
	UUID sessionId,
	UUID userId
) implements Query<TripVoteSessionResult> {
}
