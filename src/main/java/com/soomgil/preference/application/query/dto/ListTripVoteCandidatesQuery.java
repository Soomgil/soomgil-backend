package com.soomgil.preference.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.List;
import java.util.UUID;

/**
 * 여행 지역과 모든 활성 참여자의 누적 취향으로 투표 후보를 구성하는 query.
 *
 * <p>지도 viewport 기반 추천과 달리 bbox를 요구하지 않는다. 방장이 투표를 시작하는 시점처럼
 * 사용자가 지도를 보고 있지 않은 흐름에서 사용한다.
 *
 * <p>호출자는 이 결과를 투표 세션 snapshot으로 고정해, 투표 도중 추천 결과가 바뀌어도
 * 후보가 변하지 않게 해야 한다.
 *
 * <p>반환되는 {@link TripVoteCandidateView}에는 다른 참여자의 취향 점수나 태그가 포함되지 않는다.
 *
 * @param tripId 후보를 만들 여행방 ID
 * @param requesterUserId 요청 사용자 ID. active member 권한 확인에 사용된다
 * @param limit 최대 후보 수. 기본 설계값은 10이다
 * @param destinationKeyword 여행방에 등록된 지역이 없을 때 사용할 대체 검색어. 없으면 null
 */
public record ListTripVoteCandidatesQuery(
	UUID tripId,
	UUID requesterUserId,
	int limit,
	String destinationKeyword
) implements Query<List<TripVoteCandidateView>> {

	/**
	 * 대체 검색어 없이 query를 만든다.
	 *
	 * @param tripId 여행방 ID
	 * @param requesterUserId 요청 사용자 ID
	 * @param limit 최대 후보 수
	 */
	public ListTripVoteCandidatesQuery(UUID tripId, UUID requesterUserId, int limit) {
		this(tripId, requesterUserId, limit, null);
	}
}
