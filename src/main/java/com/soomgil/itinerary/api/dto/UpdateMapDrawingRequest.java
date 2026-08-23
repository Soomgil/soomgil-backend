package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 저장 지도 drawing의 낙관적 잠금 수정 요청.
 *
 * <p>HTTP 요청은 활성 WebSocket session header와 함께 보내야 하며,
 * 지도 오브젝트 transform의 크기 단위는 meter다.
 */
public record UpdateMapDrawingRequest(
	@NotNull
	Long baseVersion,
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	Map<String, Object> transform,
	Integer sortOrder,
	Long drawingVersion
) {
	public UpdateMapDrawingRequest(
		Long baseVersion,
		Map<String, Object> geometry,
		Map<String, Object> style,
		String label,
		Integer sortOrder,
		Long drawingVersion
	) {
		this(baseVersion, geometry, style, label, null, sortOrder, drawingVersion);
	}
}
