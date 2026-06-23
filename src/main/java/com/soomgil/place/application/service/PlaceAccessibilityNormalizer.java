package com.soomgil.place.application.service;

import com.soomgil.place.application.port.PlaceIntroRaw;
import com.soomgil.place.application.query.dto.AccessibilityFlag;
import com.soomgil.place.application.query.dto.ParkingType;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * KTO detailIntro의 raw 텍스트를 정규화된 {@link PlaceAccessibilityInfo}로 변환한다.
 * 규칙 기반 1차 파싱. 데이터 품질 이슈가 크면 LLM 보조를 별도 작업으로 도입한다.
 */
@Component
public class PlaceAccessibilityNormalizer {

	public PlaceAccessibilityInfo normalize(PlaceIntroRaw raw) {
		if (raw == null || raw.isEmpty()) {
			return PlaceAccessibilityInfo.unknown();
		}
		String openingHours = cleanText(raw.useTime());
		String closedDays = cleanText(raw.restDate());
		ParkingType parkingType = classifyParking(raw.parking());
		Set<AccessibilityFlag> flags = extractFlags(raw);
		return new PlaceAccessibilityInfo(openingHours, closedDays, parkingType, flags);
	}

	private String cleanText(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value
			.replaceAll("<br\\s*/?>", "\n")
			.replaceAll("<[^>]+>", " ")
			.replaceAll("&nbsp;", " ")
			.replaceAll("[ \t]+", " ")
			.trim();
		return stripped.isEmpty() ? null : stripped;
	}

	private ParkingType classifyParking(String raw) {
		if (raw == null || raw.isBlank()) {
			return ParkingType.UNKNOWN;
		}
		String lower = raw.toLowerCase();
		boolean free = raw.contains("무료") || lower.contains("free");
		boolean paid = raw.contains("유료") || lower.contains("paid") || lower.contains("fee");
		boolean none = raw.contains("불가") || raw.contains("없음") || lower.contains("no parking");
		if (none && !free && !paid) {
			return ParkingType.NONE;
		}
		if (free && paid) {
			return ParkingType.MIXED;
		}
		if (free) {
			return ParkingType.FREE;
		}
		if (paid) {
			return ParkingType.PAID;
		}
		if (raw.contains("가능") || raw.contains("주차장")) {
			return ParkingType.FREE;
		}
		return ParkingType.UNKNOWN;
	}

	private Set<AccessibilityFlag> extractFlags(PlaceIntroRaw raw) {
		Set<AccessibilityFlag> flags = new LinkedHashSet<>();
		String disability = raw.disability();
		if (disability != null && !disability.isBlank()) {
			if (hasSupportedLine(disability, "휠체어", "wheelchair")) {
				flags.add(AccessibilityFlag.WHEELCHAIR);
			}
			if (hasSupportedLine(disability, "장애인 화장실", "장애인용 화장실", "장애인화장실", "장애인용변기", "장애인용 변기")) {
				flags.add(AccessibilityFlag.DISABLED_TOILET);
			}
			if (hasSupportedLine(disability, "노약자", "어르신", "경로", "elderly", "senior")) {
				flags.add(AccessibilityFlag.ELDERLY);
			}
		}
		if (isAllowed(raw.chkBabyCarriage())) {
			flags.add(AccessibilityFlag.STROLLER);
		}
		if (isAllowed(raw.chkPet())) {
			flags.add(AccessibilityFlag.PET);
		}
		return flags;
	}

	private boolean isAllowed(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		return !isDenied(value)
			&& (value.contains("가능") || value.contains("있음") || value.contains("제공") || value.contains("대여"));
	}

	private boolean hasSupportedLine(String value, String... keywords) {
		return value.lines().anyMatch(line -> {
			String lower = line.toLowerCase();
			boolean matches = java.util.Arrays.stream(keywords)
				.anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
			return matches && !isDenied(line);
		});
	}

	private boolean isDenied(String value) {
		String lower = value.toLowerCase();
		return value.contains("불가능")
			|| value.contains("없음")
			|| value.contains("미제공")
			|| lower.contains("not available")
			|| lower.contains("no wheelchair");
	}
}
