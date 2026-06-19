package com.soomgil.trip.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 여행방 초대 aggregate.
 *
 * <p>초대 링크 token 원문은 저장하지 않고, 저장소에는 {@code inviteTokenHash}만 남긴다.
 * MVP의 code 기반 수락은 {@code inviteCode}를 안정 식별자로 사용한다.
 */
public record TripInvite(
	UUID id,
	UUID tripId,
	UUID createdByUserId,
	UUID inviteeUserId,
	String inviteCode,
	String inviteTokenHash,
	InviteStatus status,
	Instant expiresAt,
	Instant acceptedAt,
	UUID acceptedByUserId,
	Instant revokedAt,
	Instant createdAt
) {

	public TripInvite {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
		inviteCode = normalizeCode(inviteCode);
		if (inviteTokenHash == null || inviteTokenHash.isBlank()) {
			throw new IllegalArgumentException("inviteTokenHash must not be blank");
		}
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	/**
	 * 새 대기 상태 초대를 만든다.
	 *
	 * @param id 초대 ID
	 * @param tripId 여행방 ID
	 * @param createdByUserId 초대를 만든 owner 사용자 ID
	 * @param inviteeUserId 직접 초대 대상. 링크 초대면 null
	 * @param inviteCode 사용자가 입력하거나 URL에 포함되는 code
	 * @param inviteTokenHash 저장용 token hash
	 * @param expiresAt 선택 만료 시각
	 * @param createdAt 생성 시각
	 * @return PENDING 초대
	 */
	public static TripInvite createPending(
		UUID id,
		UUID tripId,
		UUID createdByUserId,
		UUID inviteeUserId,
		String inviteCode,
		String inviteTokenHash,
		Instant expiresAt,
		Instant createdAt
	) {
		return new TripInvite(
			id,
			tripId,
			createdByUserId,
			inviteeUserId,
			inviteCode,
			inviteTokenHash,
			InviteStatus.PENDING,
			expiresAt,
			null,
			null,
			null,
			createdAt
		);
	}

	private static String normalizeCode(String inviteCode) {
		if (inviteCode == null || inviteCode.isBlank()) {
			throw new IllegalArgumentException("inviteCode must not be blank");
		}
		return inviteCode.trim().toUpperCase();
	}
}
