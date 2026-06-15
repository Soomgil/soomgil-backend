package com.soomgil.global.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

	@Test
	void createsEventEnvelopeWithCommonMetadata() {
		Instant occurredAt = Instant.parse("2026-06-16T00:00:00Z");
		Clock clock = Clock.fixed(occurredAt, ZoneOffset.UTC);
		UUID actorUserId = UUID.randomUUID();
		TripCreatedPayload payload = new TripCreatedPayload(UUID.randomUUID(), "Jeju");

		EventEnvelope<TripCreatedPayload> envelope = EventEnvelope.create(
			"trip.created",
			"trip",
			payload.tripId().toString(),
			actorUserId,
			payload,
			clock
		);

		assertThat(envelope.eventId()).isNotNull();
		assertThat(envelope.eventType()).isEqualTo("trip.created");
		assertThat(envelope.schemaVersion()).isEqualTo(1);
		assertThat(envelope.occurredAt()).isEqualTo(occurredAt);
		assertThat(envelope.aggregateType()).isEqualTo("trip");
		assertThat(envelope.aggregateId()).isEqualTo(payload.tripId().toString());
		assertThat(envelope.actorUserId()).isEqualTo(actorUserId);
		assertThat(envelope.payload()).isEqualTo(payload);
	}

	@Test
	void rejectsBlankEventType() {
		assertThatThrownBy(() -> new EventEnvelope<>(
			UUID.randomUUID(),
			" ",
			1,
			Instant.now(),
			"trip",
			"trip-1",
			null,
			new TripCreatedPayload(UUID.randomUUID(), "Jeju")
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsSchemaVersionLessThanOne() {
		assertThatThrownBy(() -> new EventEnvelope<>(
			UUID.randomUUID(),
			"trip.created",
			0,
			Instant.now(),
			"trip",
			"trip-1",
			null,
			new TripCreatedPayload(UUID.randomUUID(), "Jeju")
		)).isInstanceOf(IllegalArgumentException.class);
	}

	private record TripCreatedPayload(
		UUID tripId,
		String title
	) {
	}
}
