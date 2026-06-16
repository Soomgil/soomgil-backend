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

	/**
	 * 여행방 멤버십을 추가한다.
	 *
	 * @param member 추가할 active member
	 */
	void addTripMember(TripMember member);

	/**
	 * 초대를 수락 상태로 전환한다.
	 *
	 * @param inviteId 초대 ID
	 * @param acceptedByUserId 수락한 사용자 ID
	 * @param acceptedAt 수락 시각
	 */
	void acceptTripInvite(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt);

	/**
	 * 여행방 기본 설정을 갱신한다.
	 *
	 * @param update 갱신할 설정 값
	 */
	void updateTrip(TripSettingsUpdate update);

	/**
	 * 여행방 법정동 code 목록을 교체한다.
	 *
	 * @param tripId 여행방 ID
	 * @param legalRegionCodes 새 법정동 code 목록
	 * @param createdAt 새 region row 생성 시각
	 */
	void replaceTripRegions(UUID tripId, List<String> legalRegionCodes, Instant createdAt);

	/**
	 * 여행방을 soft delete 상태로 전환한다.
	 *
	 * @param tripId 여행방 ID
	 * @param deletedAt 삭제 시각
	 */
	void softDeleteTrip(UUID tripId, Instant deletedAt);

	/**
	 * active 멤버를 제거 상태로 전환한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 제거할 사용자 ID
	 * @param removedByUserId 제거한 owner 사용자 ID
	 * @param removedAt 제거 시각
	 */
	void removeTripMember(UUID tripId, UUID userId, UUID removedByUserId, Instant removedAt);
}
