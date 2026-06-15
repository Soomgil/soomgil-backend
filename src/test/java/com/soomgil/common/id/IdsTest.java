package com.soomgil.common.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTest {

	@Test
	void createsUuid() {
		UUID id = Ids.newUuid();

		assertThat(id).isNotNull();
	}

	@Test
	void parsesUuidString() {
		UUID id = UUID.randomUUID();

		assertThat(Ids.parseUuid(id.toString(), "tripId")).isEqualTo(id);
	}

	@Test
	void rejectsInvalidUuidString() {
		assertThatThrownBy(() -> Ids.parseUuid("not-a-uuid", "tripId"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("tripId");
	}

	@Test
	void rejectsBlankUuidString() {
		assertThatThrownBy(() -> Ids.parseUuid(" ", "tripId"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("tripId");
	}
}
