package com.soomgil.collaboration.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 같은 여행방에 중계되는 지도 오브젝트 transform preview.
 *
 * @param eventType 고정 이벤트 타입
 * @param tripId 여행방 ID
 * @param drawingId 지도 오브젝트 ID
 * @param userId 편집 사용자 ID
 * @param clientId 편집 WebSocket session ID
 * @param sequence 오브젝트별 증가 순번
 * @param phase UPDATE, END, CANCEL 중 하나
 * @param transform 임시 transform
 * @param sentAt 서버 중계 시각
 */
public record MapObjectTransformPreviewEvent(
	String eventType,
	UUID tripId,
	UUID drawingId,
	UUID userId,
	String clientId,
	long sequence,
	String phase,
	Map<String, Object> transform,
	Instant sentAt
) {
	/** 서버가 검증한 송신자 정보로 preview 이벤트를 만든다. */
	public static MapObjectTransformPreviewEvent of(
		UUID tripId,
		UUID userId,
		String clientId,
		MapObjectTransformPreviewRequest request,
		Instant sentAt
	) {
		return new MapObjectTransformPreviewEvent(
			"map.object.transform.preview", tripId, request.drawingId(), userId, clientId,
			request.sequence(), request.phase(), request.transform(), sentAt
		);
	}
}
