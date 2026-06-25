package com.soomgil.place.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.place.application.port.PlaceIntroRaw;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.query.dto.AccessibilityFlag;
import com.soomgil.place.application.query.dto.ParkingType;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.infrastructure.persistence.repository.PlaceAccessibilityOverrideRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlaceAccessibilityCacheBackendTest {

	@Test
	void returnsSeededOverrideBeforeCallingKto() {
		TourismPlaceFeedClient client = Mockito.mock(TourismPlaceFeedClient.class);
		PlaceAccessibilityOverrideRepository overrides = Mockito.mock(PlaceAccessibilityOverrideRepository.class);
		PlaceAccessibilityInfo seeded = new PlaceAccessibilityInfo(
			null,
			null,
			ParkingType.FREE,
			Set.of(AccessibilityFlag.WHEELCHAIR),
			Set.of()
		);
		when(overrides.find("KTO", "228853")).thenReturn(Optional.of(seeded));

		PlaceAccessibilityCacheBackend backend = new PlaceAccessibilityCacheBackend(
			client,
			new PlaceAccessibilityNormalizer(),
			overrides
		);

		PlaceAccessibilityInfo result = backend.load("KTO", "228853", "12");

		assertThat(result).isEqualTo(seeded);
		verify(client, never()).fetchIntro("228853", "12");
	}

	@Test
	void fallsBackToKtoWhenOverrideIsMissing() {
		TourismPlaceFeedClient client = Mockito.mock(TourismPlaceFeedClient.class);
		PlaceAccessibilityOverrideRepository overrides = Mockito.mock(PlaceAccessibilityOverrideRepository.class);
		when(overrides.find("KTO", "126508")).thenReturn(Optional.empty());
		when(client.fetchIntro("126508", "12")).thenReturn(new PlaceIntroRaw(
			null,
			null,
			"무료 주차 가능",
			"휠체어 접근 가능",
			null,
			null
		));

		PlaceAccessibilityCacheBackend backend = new PlaceAccessibilityCacheBackend(
			client,
			new PlaceAccessibilityNormalizer(),
			overrides
		);

		PlaceAccessibilityInfo result = backend.load("KTO", "126508", "12");

		assertThat(result.flags()).containsExactly(AccessibilityFlag.WHEELCHAIR);
		verify(client).fetchIntro("126508", "12");
	}
}
