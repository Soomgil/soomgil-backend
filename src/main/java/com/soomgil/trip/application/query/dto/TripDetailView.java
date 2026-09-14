package com.soomgil.trip.application.query.dto;

import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import java.util.UUID;

/**
 * 여행방 상세 view.
 *
 * <p>멤버 목록은 현재 user profile 저장소와 연결되기 전까지 userId 기반 view로 반환한다.
 * {@code regions}는 여행방에 등록한 순서대로의 법정동 지역이며, geo 모듈의 공개 query로 이름을 채운다.
 */
public record TripDetailView(
	UUID id,
	String title,
	String displayDestination,
	TripStatus status,
	TripAccessRole myRole,
	long itineraryVersion,
	Instant createdAt,
	UUID ownerUserId,
	List<TripMemberView> members,
	UUID retrippedFromPostId,
	List<LegalRegionView> regions
) {

	public TripDetailView {
		members = members == null ? List.of() : List.copyOf(members);
		regions = regions == null ? List.of() : List.copyOf(regions);
	}

	/**
	 * 지역 없이 만든다. 지역을 아직 조회하지 않는 호출자와 기존 테스트 호환용이다.
	 */
	public TripDetailView(
		UUID id,
		String title,
		String displayDestination,
		TripStatus status,
		TripAccessRole myRole,
		long itineraryVersion,
		Instant createdAt,
		UUID ownerUserId,
		List<TripMemberView> members,
		UUID retrippedFromPostId
	) {
		this(id, title, displayDestination, status, myRole, itineraryVersion, createdAt, ownerUserId, members,
			retrippedFromPostId, List.of());
	}
}
