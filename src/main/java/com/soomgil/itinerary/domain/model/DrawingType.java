package com.soomgil.itinerary.domain.model;

/**
 * 저장된 지도 도형 유형.
 *
 * <p>STICKER와 IMAGE는 지도 좌표 중심, meter 크기, 회전 transform을 함께 저장한다.
 */
public enum DrawingType {
	FREEHAND,
	LINE,
	POLYGON,
	MARKER,
	TEXT,
	STICKER,
	IMAGE
}
