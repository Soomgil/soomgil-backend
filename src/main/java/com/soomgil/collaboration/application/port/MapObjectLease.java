package com.soomgil.collaboration.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * 지도 오브젝트를 편집할 수 있는 WebSocket session의 임시 lease.
 *
 * @param tripId 여행방 ID
 * @param drawingId 지도 오브젝트 ID
 * @param userId 편집 사용자 ID
 * @param websocketSessionId 편집 권한을 소유한 WebSocket session ID
 * @param expiresAt lease 만료 시각
 */
public record MapObjectLease(
	UUID tripId,
	UUID drawingId,
	UUID userId,
	String websocketSessionId,
	Instant expiresAt
) {
}
