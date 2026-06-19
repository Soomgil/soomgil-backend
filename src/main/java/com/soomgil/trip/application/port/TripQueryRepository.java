package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 여행방 읽기 persistence 계약.
 *
 * <p>다른 모듈이 필요한 권한 판정용 데이터는 이 repository를 통해 조회하고,
 * application handler에서 접근 view로 변환한다.
 */
public interface TripQueryRepository {

	/**
	 * 특정 사용자와 여행방의 접근 판정에 필요한 snapshot을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 사용자 ID
	 * @return 여행방이 존재하면 snapshot, 없으면 empty
	 */
	Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId);

	/**
	 * 여행방 상세 조회에 필요한 기본 정보를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 여행방이 존재하면 read model
	 */
	Optional<TripReadModel> findTrip(UUID tripId);

	/**
	 * 여행방 멤버 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param status 선택적 멤버 상태 필터
	 * @return 멤버 목록
	 */
	List<TripMemberReadModel> findTripMembers(UUID tripId, TripMemberStatus status);

	/**
	 * 현재 사용자가 active member인 여행방 목록을 조회한다.
	 *
	 * @param userId 현재 사용자 ID
	 * @param status 선택적 여행방 상태 필터
	 * @param role 선택적 파생 access role 필터
	 * @param page 0 기반 page
	 * @param size page 크기
	 * @param sort 정렬 문자열 목록
	 * @return page 목록과 전체 개수
	 */
	TripSummaryPage findMyTrips(
		UUID userId,
		TripStatus status,
		TripAccessRole role,
		int page,
		int size,
		List<String> sort
	);

	/**
	 * 여행방 초대 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param status 선택적 초대 상태
	 * @return 초대 목록
	 */
	List<TripInviteReadModel> findTripInvites(UUID tripId, InviteStatus status);

	/**
	 * 초대 수락에 필요한 초대와 여행방 상태를 조회한다.
	 *
	 * @param inviteCode 초대 code
	 * @return 수락 검증용 read model
	 */
	Optional<TripInviteAcceptReadModel> findTripInviteForAccept(String inviteCode);
}
