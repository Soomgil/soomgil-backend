package com.soomgil.trip.domain.policy;

import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.UUID;

/**
 * 여행방 접근 권한을 계산하는 정책.
 *
	 * <p>권한 판단은 삭제되지 않은 trip, active membership, ownerUserId 세 가지 값으로만 수행한다.
 * MVP에서는 trip_members.role에 OWNER를 저장하지 않는다.
 */
public final class TripAccessPolicy {

	private TripAccessPolicy() {
	}

	/**
	 * 저장소 snapshot을 다른 모듈에서 사용할 수 있는 access view로 변환한다.
	 *
	 * @param tripId 요청 여행방 ID
	 * @param userId 요청 사용자 ID
	 * @param snapshot 저장소 조회 결과. 여행방이 없으면 null 가능
	 * @return 접근 가능 여부와 파생 role
	 */
	public static TripAccessView evaluate(UUID tripId, UUID userId, TripAccessSnapshot snapshot) {
		if (snapshot == null || snapshot.tripStatus() == TripStatus.DELETED) {
			return TripAccessView.denied(tripId, userId);
		}

		boolean activeMember = snapshot.memberStatus() == TripMemberStatus.ACTIVE;
		if (!activeMember) {
			return TripAccessView.denied(tripId, userId);
		}

		boolean owner = snapshot.ownerUserId().equals(userId);
		TripAccessRole role = owner ? TripAccessRole.OWNER : TripAccessRole.MEMBER;
		return new TripAccessView(tripId, userId, true, activeMember, owner, role);
	}
}
