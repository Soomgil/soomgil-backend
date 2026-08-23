package com.soomgil.collaboration.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 인증된 WebSocket session의 지도 커서 이벤트.
 *
 * @param eventType 고정 이벤트 타입
 * @param tripId 여행방 ID
 * @param userId 서버 인증 사용자 ID
 * @param clientId 서버 WebSocket session ID
 * @param longitude 경도
 * @param latitude 위도
 * @param sequence session sequence
 * @param sentAt 서버 전송 시각
 */
public record MapCursorEvent(
	String eventType,
	UUID tripId,
	UUID userId,
	String clientId,
	double longitude,
	double latitude,
	long sequence,
	Instant sentAt
) {
}
