package com.soomgil.itinerary.api.dto;

/** 지도 drawing API 타입. STICKER와 IMAGE는 meter 기반 transform을 사용한다. */
public enum DrawingType {
	FREEHAND,
	LINE,
	POLYGON,
	MARKER,
	TEXT,
	STICKER,
	IMAGE
}
