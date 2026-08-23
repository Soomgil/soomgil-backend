package com.soomgil.itinerary.domain.policy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.domain.model.DrawingType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 지도 drawing subtype별 필수 필드와 지도 크기 transform 불변 조건을 검증한다.
 *
 * <p>스티커와 이미지는 화면 픽셀이 아닌 지도상의 meter 크기로 저장한다.
 */
@Component
public final class MapDrawingObjectPolicy {

	private static final double MAX_SIZE_METERS = 1_000_000D;
	private static final Set<String> STICKER_CODES = Set.of(
		"HEART", "STAR", "CHECK", "CAMERA", "FOOD", "CAFE",
		"SHOPPING", "HOTEL", "NATURE", "BEACH", "MUSEUM", "TRANSPORT"
	);

	/**
	 * drawing subtype별 media/sticker 필드 배타성과 transform 범위를 검증한다.
	 *
	 * @param drawingType drawing 타입
	 * @param mediaFileId IMAGE가 참조할 MAP_OVERLAY 미디어 ID
	 * @param stickerCode STICKER가 참조할 기본 스티커 코드
	 * @param transform 지도상 크기와 회전 transform
	 */
	public void validate(
		DrawingType drawingType,
		UUID mediaFileId,
		String stickerCode,
		Map<String, Object> transform
	) {
		if (drawingType == null) {
			fail("Drawing type is required.");
		}
		if (drawingType == DrawingType.STICKER) {
			if (mediaFileId != null || stickerCode == null || !STICKER_CODES.contains(stickerCode)) {
				fail("A valid stickerCode is required for STICKER.");
			}
			validateTransform(transform);
			return;
		}
		if (drawingType == DrawingType.IMAGE) {
			if (mediaFileId == null || stickerCode != null) {
				fail("A mediaFileId is required for IMAGE.");
			}
			validateTransform(transform);
			return;
		}
		if (mediaFileId != null || stickerCode != null || transform != null) {
			fail("Media, sticker, and transform fields are only allowed for map objects.");
		}
	}

	/** 등록된 기본 스티커 code인지 확인한다. */
	public boolean isSupportedStickerCode(String stickerCode) {
		return STICKER_CODES.contains(stickerCode);
	}

	private void validateTransform(Map<String, Object> transform) {
		if (transform == null || transform.isEmpty()) {
			fail("Transform is required for map objects.");
		}
		double width = number(transform.get("widthMeters"), "widthMeters");
		double height = number(transform.get("heightMeters"), "heightMeters");
		double rotation = number(transform.get("rotationDeg"), "rotationDeg");
		double centerLng = number(transform.get("centerLng"), "centerLng");
		double centerLat = number(transform.get("centerLat"), "centerLat");
		if (width <= 0 || width > MAX_SIZE_METERS || height <= 0 || height > MAX_SIZE_METERS) {
			fail("Map object width and height must be positive map distances.");
		}
		if (!Double.isFinite(rotation)) {
			fail("Map object rotation must be finite.");
		}
		if (centerLng < -180 || centerLng > 180 || centerLat < -90 || centerLat > 90) {
			fail("Map object center must be a valid longitude and latitude.");
		}
	}

	private double number(Object value, String name) {
		if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
			fail(name + " must be a finite number.");
		}
		return ((Number) value).doubleValue();
	}

	private void fail(String message) {
		throw new BusinessException(ErrorCode.VALIDATION_FAILED, message);
	}
}
