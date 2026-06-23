package com.soomgil.place.application.query.dto;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 정규화된 장소 접근성/운영시간 정보.
 * KTO detailIntro의 자유 텍스트를 규칙 기반으로 변환한 결과.
 */
public record PlaceAccessibilityInfo(
	String openingHours,
	String closedDays,
	ParkingType parkingType,
	Set<AccessibilityFlag> flags
) implements Serializable {
	private static final long serialVersionUID = 1L;

	public static PlaceAccessibilityInfo unknown() {
		return new PlaceAccessibilityInfo(null, null, ParkingType.UNKNOWN, Set.of());
	}

	public PlaceAccessibilityInfo {
		flags = flags == null ? Set.of() : Set.copyOf(flags);
	}

	public PlaceAccessibilityInfo withFlags(Set<AccessibilityFlag> additional) {
		Set<AccessibilityFlag> merged = new LinkedHashSet<>(flags);
		if (additional != null) {
			merged.addAll(additional);
		}
		return new PlaceAccessibilityInfo(openingHours, closedDays, parkingType, merged);
	}
}
