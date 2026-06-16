package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripInvite;
import com.soomgil.trip.domain.model.TripMember;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 쓰기 persistence 계약.
 *
 * <p>구현체는 여행방 생성과 최초 멤버십 저장을 같은 transaction 안에서 수행해야 한다.
 */
public interface TripCommandRepository {

	/**
	 * 생성된 여행방과 생성자의 최초 멤버십을 저장한다.
	 *
	 * @param trip 저장할 여행방
	 * @param initialMember 생성자 active member
	 * @param legalRegionCodes 선택적 법정동 코드 목록
	 */
	void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes);

	/**
	 * 여행방 초대를 저장한다.
	 *
	 * @param invite 저장할 초대
	 */
	void saveTripInvite(TripInvite invite);

	/**
	 * 대기 중인 초대를 취소한다.
	 *
	 * @param inviteId 초대 ID
	 * @param revokedByUserId 취소한 사용자 ID
	 * @param revokedAt 취소 시각
	 */
	void revokeTripInvite(UUID inviteId, UUID revokedByUserId, Instant revokedAt);
}
