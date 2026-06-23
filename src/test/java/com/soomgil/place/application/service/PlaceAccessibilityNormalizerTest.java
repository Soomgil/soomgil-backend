package com.soomgil.place.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.place.application.port.PlaceIntroRaw;
import com.soomgil.place.application.query.dto.AccessibilityFlag;
import com.soomgil.place.application.query.dto.ParkingType;
import org.junit.jupiter.api.Test;

class PlaceAccessibilityNormalizerTest {

	private final PlaceAccessibilityNormalizer normalizer = new PlaceAccessibilityNormalizer();

	@Test
	void normalizesKtoIntroTextIntoOptionalAccessibilityInformation() {
		var result = normalizer.normalize(new PlaceIntroRaw(
			"09:00~18:00<br>입장 마감 17:30",
			"매주 월요일",
			"무료 주차 가능",
			"휠체어 및 장애인 화장실 이용 가능",
			"유모차 대여 가능",
			"반려동물 동반 불가능"
		));

		assertThat(result.openingHours()).isEqualTo("09:00~18:00\n입장 마감 17:30");
		assertThat(result.closedDays()).isEqualTo("매주 월요일");
		assertThat(result.parkingType()).isEqualTo(ParkingType.FREE);
		assertThat(result.flags()).containsExactlyInAnyOrder(
			AccessibilityFlag.WHEELCHAIR,
			AccessibilityFlag.DISABLED_TOILET,
			AccessibilityFlag.STROLLER
		);
		assertThat(result.unavailableFlags()).containsExactly(AccessibilityFlag.PET);
	}

	@Test
	void returnsUnknownWhenKtoIntroIsMissing() {
		var result = normalizer.normalize(PlaceIntroRaw.empty());

		assertThat(result.openingHours()).isNull();
		assertThat(result.closedDays()).isNull();
		assertThat(result.parkingType()).isEqualTo(ParkingType.UNKNOWN);
		assertThat(result.flags()).isEmpty();
		assertThat(result.unavailableFlags()).isEmpty();
	}

	@Test
	void doesNotExposeUnavailableBarrierFreeFacilitiesAsSupported() {
		var result = normalizer.normalize(new PlaceIntroRaw(
			null,
			null,
			null,
			"휠체어 접근 불가능\n장애인 화장실 없음",
			"유모차 대여 불가능",
			"반려동물 동반 불가능"
		));

		assertThat(result.flags()).isEmpty();
		assertThat(result.unavailableFlags()).containsExactlyInAnyOrder(
			AccessibilityFlag.WHEELCHAIR,
			AccessibilityFlag.DISABLED_TOILET,
			AccessibilityFlag.STROLLER,
			AccessibilityFlag.PET
		);
	}
}
