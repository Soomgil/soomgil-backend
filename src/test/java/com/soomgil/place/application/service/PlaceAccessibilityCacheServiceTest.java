package com.soomgil.place.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceAccessibilityCacheServiceTest {

	@Test
	void returnsUnknownForFailedBatchItems() {
		PlaceAccessibilityCacheBackend backend = mock(PlaceAccessibilityCacheBackend.class);
		PlaceAccessibilityCacheService service = new PlaceAccessibilityCacheService(backend);
		when(backend.load("KTO", "missing", null)).thenThrow(new IllegalStateException("missing"));

		var result = service.getMany(List.of(new PlaceAccessibilityCacheService.PlaceRef("KTO", "missing", null)));

		assertThat(result).containsKey("KTO:missing");
		assertThat(result.get("KTO:missing")).isEqualTo(
			com.soomgil.place.application.query.dto.PlaceAccessibilityInfo.unknown()
		);
	}
}
