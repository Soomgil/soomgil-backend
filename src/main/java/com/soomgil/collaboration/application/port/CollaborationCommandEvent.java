package com.soomgil.collaboration.application.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 협업 write command audit/event 저장 모델.
 *
 * <p>{@code payload}, {@code inversePayload}, {@code redoPayload}는 JSON 문자열이며 repository가 jsonb로 저장한다.
 */
public record CollaborationCommandEvent(
	UUID tripId,
	UUID actorUserId,
	String websocketSessionId,
	String source,
	String commandType,
	String aggregateType,
	UUID aggregateId,
	Long versionBefore,
	Long versionAfter,
	String payload,
	String inversePayload,
	String redoPayload,
	Instant createdAt
) {

	public CollaborationCommandEvent {
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(actorUserId, "actorUserId must not be null");
		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(commandType, "commandType must not be null");
		Objects.requireNonNull(aggregateType, "aggregateType must not be null");
		Objects.requireNonNull(payload, "payload must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}
}
