package com.soomgil.collaboration.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 여행방에 전파되는 지도 오브젝트 편집 잠금 상태.
 *
 * @param eventType 고정 이벤트 타입
 * @param tripId 여행방 ID
 * @param drawingId 지도 오브젝트 ID
 * @param locked 잠금 활성 여부
 * @param userId 잠금 사용자 ID. 해제 이벤트에서는 null
 * @param clientId 잠금 WebSocket session ID. 해제 이벤트에서는 null
 * @param expiresAt lease 만료 시각. 해제 이벤트에서는 null
 */
public record MapObjectLockEvent(
	String eventType,
	UUID tripId,
	UUID drawingId,
	boolean locked,
	UUID userId,
	String clientId,
	Instant expiresAt
) {
	public static MapObjectLockEvent locked(
		UUID tripId,
		UUID drawingId,
		UUID userId,
		String clientId,
		Instant expiresAt
	) {
		return new MapObjectLockEvent("map.object.lock", tripId, drawingId, true, userId, clientId, expiresAt);
	}

	public static MapObjectLockEvent released(UUID tripId, UUID drawingId) {
		return new MapObjectLockEvent("map.object.lock", tripId, drawingId, false, null, null, null);
	}
}
