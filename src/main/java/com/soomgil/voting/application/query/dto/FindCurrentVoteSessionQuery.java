package com.soomgil.voting.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.voting.api.dto.TripVoteSessionState;
import java.util.UUID;

/**
 * 여행 방 진입 시 보여줄 화면을 판정하기 위한 query.
 *
 * <p>프론트 라우터 가드가 호출하는 단일 진입점이다. active member면 누구나 호출할 수 있고,
 * 세션이 없거나 참여자가 아니면 지도 화면으로 안내한다.
 *
 * @param tripId 여행방 식별자
 * @param userId 요청 사용자 식별자
 */
public record FindCurrentVoteSessionQuery(
	UUID tripId,
	UUID userId
) implements Query<TripVoteSessionState> {
}
