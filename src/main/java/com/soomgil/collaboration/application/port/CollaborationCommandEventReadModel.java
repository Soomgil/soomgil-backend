package com.soomgil.collaboration.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * 협업 command event 조회 모델.
 */
public record CollaborationCommandEventReadModel(
	Long id,
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
}
