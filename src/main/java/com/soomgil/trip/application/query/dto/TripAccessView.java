package com.soomgil.trip.application.query.dto;

import com.soomgil.trip.domain.model.TripAccessRole;
import java.util.UUID;

/**
 * 여행방 접근 권한 조회 결과.
 *
 * <p>{@code canAccess}가 false이면 {@code accessRole}은 null이다. 호출 모듈은
 * 이 view의 boolean 값만 사용하고 trip_members.role을 직접 해석하지 않는다.
 */
public record TripAccessView(
	UUID tripId,
	UUID userId,
	boolean canAccess,
	boolean activeMember,
	boolean owner,
	TripAccessRole accessRole
) {

	/**
	 * 접근 거부 결과를 만든다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 사용자 ID
	 * @return 접근 불가 view
	 */
	public static TripAccessView denied(UUID tripId, UUID userId) {
		return new TripAccessView(tripId, userId, false, false, false, null);
	}
}
