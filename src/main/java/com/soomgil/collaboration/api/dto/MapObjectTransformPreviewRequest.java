package com.soomgil.collaboration.api.dto;

import java.util.Map;
import java.util.UUID;

/**
 * 지도 오브젝트 조작 중 전송하는 임시 transform.
 *
 * @param drawingId 지도 오브젝트 ID
 * @param sequence WebSocket 세션 내 오브젝트별 증가 순번
 * @param phase UPDATE, END, CANCEL 중 하나
 * @param transform 화면에 즉시 반영할 지도 좌표 기반 transform
 */
public record MapObjectTransformPreviewRequest(
	UUID drawingId,
	long sequence,
	String phase,
	Map<String, Object> transform
) {
}
