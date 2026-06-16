package com.soomgil.trip.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 여행방 aggregate의 최소 생성 상태.
 *
 * <p>초대, 일정, 기록은 별도 use case에서 확장한다. 이 모델은 여행방 생성 시
 * owner, 제목, 대표 목적지, 상태, 일정 버전의 초기값을 고정한다.
 */
public record Trip(
	UUID id,
	UUID ownerUserId,
	String title,
	String displayDestination,
	TripStatus status,
	long itineraryVersion,
	Instant createdAt
) {

	public Trip {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
		title = TripTitlePolicy.normalizeTitle(title);
		displayDestination = TripTitlePolicy.normalizeOptionalText(displayDestination);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		if (itineraryVersion < 0) {
			throw new IllegalArgumentException("itineraryVersion must not be negative");
		}
	}

	/**
	 * 새 여행방의 초기 aggregate를 만든다.
	 *
	 * @param id 여행방 ID
	 * @param ownerUserId 생성자이자 owner 사용자 ID
	 * @param title 사용자 입력 제목
	 * @param displayDestination 선택적 대표 목적지
	 * @param createdAt 생성 시각
	 * @return ACTIVE 상태와 version 0을 가진 여행방
	 */
	public static Trip create(
		UUID id,
		UUID ownerUserId,
		String title,
		String displayDestination,
		Instant createdAt
	) {
		return new Trip(
			id,
			ownerUserId,
			title,
			displayDestination,
			TripStatus.ACTIVE,
			0L,
			createdAt
		);
	}

	/**
	 * 요청 사용자가 이 여행방의 owner인지 확인한다.
	 *
	 * @param userId 요청 사용자 ID
	 * @return owner이면 true
	 */
	public boolean ownedBy(UUID userId) {
		return ownerUserId.equals(userId);
	}
}
