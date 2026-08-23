package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * 일정 지도에 영구 저장된 drawing 읽기 모델.
 *
 * <p>{@code STICKER}/{@code IMAGE}는 화면 픽셀 대신 지도 중심과 meter 크기를 담은
 * {@code transform}을 사용하므로 클라이언트가 현재 지도 투영에 맞춰 렌더링한다.
 */
public record MapDrawing(
	@NotNull
	UUID id,
	UUID itineraryDayId,
	@NotNull
	DrawingType drawingType,
	@NotNull
	GeometryFormat geometryFormat,
	@NotNull
	Map<String, Object> geometry,
	Map<String, Object> style,
	String label,
	UUID mediaFileId,
	String stickerCode,
	Map<String, Object> transform,
	Integer sortOrder,
	@NotNull
	Long version
) {
	public MapDrawing(
		UUID id,
		UUID itineraryDayId,
		DrawingType drawingType,
		GeometryFormat geometryFormat,
		Map<String, Object> geometry,
		Map<String, Object> style,
		String label,
		Integer sortOrder,
		Long version
	) {
		this(id, itineraryDayId, drawingType, geometryFormat, geometry, style, label, null, null, null, sortOrder, version);
	}
}
