package com.soomgil.global.event;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventEnvelope<T>(
	UUID eventId,
	String eventType,
	int schemaVersion,
	Instant occurredAt,
	String aggregateType,
	String aggregateId,
	UUID actorUserId,
	T payload
) {

	public EventEnvelope {
		Objects.requireNonNull(eventId, "eventId must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(payload, "payload must not be null");
		eventType = requireNotBlank(eventType, "eventType");
		aggregateType = requireNotBlank(aggregateType, "aggregateType");
		aggregateId = requireNotBlank(aggregateId, "aggregateId");
		if (schemaVersion < 1) {
			throw new IllegalArgumentException("schemaVersion must be greater than or equal to 1");
		}
	}

	public static <T> EventEnvelope<T> create(
		String eventType,
		String aggregateType,
		String aggregateId,
		UUID actorUserId,
		T payload,
		Clock clock
	) {
		return new EventEnvelope<>(
			UUID.randomUUID(),
			eventType,
			1,
			Instant.now(clock),
			aggregateType,
			aggregateId,
			actorUserId,
			payload
		);
	}

	private static String requireNotBlank(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value;
	}
}
