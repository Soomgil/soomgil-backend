package com.soomgil.trip.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 여행방 멤버십 row의 도메인 표현.
 *
 * <p>MVP에서는 owner도 {@link TripMemberRole#MEMBER}로 저장하고, 접근 role은
 * {@link Trip#ownerUserId()} 기준으로 파생한다.
 */
public record TripMember(
	UUID id,
	UUID tripId,
	UUID userId,
	TripMemberRole role,
	TripMemberStatus status,
	Instant joinedAt
) {

	public TripMember {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(role, "role must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(joinedAt, "joinedAt must not be null");
	}

	/**
	 * 여행방 생성자를 최초 active member로 등록하는 멤버십을 만든다.
	 *
	 * @param id 멤버십 ID
	 * @param tripId 여행방 ID
	 * @param creatorUserId 생성자 사용자 ID
	 * @param joinedAt 가입 시각
	 * @return MEMBER role과 ACTIVE 상태의 멤버십
	 */
	public static TripMember initialOwnerMember(UUID id, UUID tripId, UUID creatorUserId, Instant joinedAt) {
		return activeMember(id, tripId, creatorUserId, joinedAt);
	}

	/**
	 * 일반 사용자를 active member로 등록하는 멤버십을 만든다.
	 *
	 * @param id 멤버십 ID
	 * @param tripId 여행방 ID
	 * @param userId 사용자 ID
	 * @param joinedAt 가입 시각
	 * @return MEMBER role과 ACTIVE 상태의 멤버십
	 */
	public static TripMember activeMember(UUID id, UUID tripId, UUID userId, Instant joinedAt) {
		return new TripMember(
			id,
			tripId,
			userId,
			TripMemberRole.MEMBER,
			TripMemberStatus.ACTIVE,
			joinedAt
		);
	}
}
