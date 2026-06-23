package com.soomgil.place.application.query.dto;

/**
 * 장소 접근성 플래그. KTO의 disability/chkbabycarriage/chkpet 필드에서 추출된 불린 속성.
 */
public enum AccessibilityFlag {
	WHEELCHAIR,
	PET,
	STROLLER,
	DISABLED_TOILET,
	ELDERLY
}
