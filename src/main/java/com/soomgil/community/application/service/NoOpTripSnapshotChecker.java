package com.soomgil.community.application.service;

import com.soomgil.community.api.dto.CommunityPostSnapshot;
import java.util.List;
import java.util.UUID;

/**
 * {@link TripSnapshotChecker}의 stub 구현.
 *
 * <p>여행방(trip) 모듈이 구현되기 전까지 사용된다. 실제 여행방 데이터를 검증하지 않고
 * 빈 snapshot을 반환한다.
 *
 * <p>{@link com.soomgil.community.application.config.CommunityModuleConfig}에서
 * {@code @ConditionalOnMissingBean}으로 등록되므로, trip 모듈에서
 * {@link TripSnapshotChecker}를 구현한 bean을 등록하면 이 stub은 자동으로 비활성화된다.
 */
public class NoOpTripSnapshotChecker implements TripSnapshotChecker {

	@Override
	public CommunityPostSnapshot fetchSnapshot(
		UUID sourceTripId,
		long baseVersion,
		UUID publisherUserId
	) {
		// TODO: trip 모듈 완성 후 실제 구현체로 교체
		// 현재는 빈 snapshot 반환. 발행자의 권한 검증과 version 충돌 검증도 생략.
		return new CommunityPostSnapshot(List.of(), List.of(), null);
	}
}
