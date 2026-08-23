package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * 지도 drawing 또는 지도 좌표 기반 오브젝트 생성 요청.
 *
 * <p>{@code STICKER}는 {@code stickerCode}, {@code IMAGE}는 여행방에 연결된 {@code mediaFileId}가 필요하다.
 * 두 타입의 {@code transform}은 중심 경·위도, meter 단위 크기, 회전 각도를 포함해야 한다.
 */
public record CreateMapDrawingRequest(
	@NotNull
	Long baseVersion,
	UUID itineraryDayId,
	@NotNull
	DrawingType drawingType,
	@NotNull
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	UUID mediaFileId,
	String stickerCode,
	Map<String, Object> transform,
	Integer sortOrder
) {
	public CreateMapDrawingRequest(
		Long baseVersion,
		UUID itineraryDayId,
		DrawingType drawingType,
		Map<String, Object> geometry,
		Map<String, Object> style,
		String label,
		Integer sortOrder
	) {
		this(baseVersion, itineraryDayId, drawingType, geometry, style, label, null, null, null, sortOrder);
	}
}
