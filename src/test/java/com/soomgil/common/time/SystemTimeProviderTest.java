package com.soomgil.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemTimeProviderTest {

	@Test
	void returnsCurrentInstantFromClock() {
		Instant fixedNow = Instant.parse("2026-06-16T00:00:00Z");
		SystemTimeProvider timeProvider = new SystemTimeProvider(Clock.fixed(fixedNow, ZoneOffset.UTC));

		assertThat(timeProvider.now()).isEqualTo(fixedNow);
		assertThat(timeProvider.clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}
}
