package com.soomgil.community.application.service;

import com.soomgil.community.api.dto.CommunityPostSnapshot;
import java.util.UUID;

/**
 * 게시글 발행 시 원본 여행방의 snapshot을 가져온다.
 *
 * <p>실제 구현체는 여행방 멤버 권한과 일정 version을 검증한 뒤 발행 시점 snapshot을 반환한다.
 */
public interface TripSnapshotChecker {

	/**
	 * 발행자가 해당 여행방의 active member인지 검증하고, snapshot을 반환한다.
	 *
	 * @param sourceTripId 원본 여행방 식별자
	 * @param baseVersion 발행자가 읽은 여행방 version (낙관적 동시성)
	 * @param publisherUserId 발행자
	 * @return 여행방 snapshot (days, routes 등)
	 * @throws com.soomgil.community.domain.model.CommunityException
	 *         권한 부족({@code TRIP_MEMBER_REQUIRED}) 또는 version 충돌
	 *         ({@code SOURCE_TRIP_VERSION_CONFLICT}) 시
	 */
	CommunityPostSnapshot fetchSnapshot(
		UUID sourceTripId,
		long baseVersion,
		UUID publisherUserId
	);
}
