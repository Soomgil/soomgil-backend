package com.soomgil.collaboration.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket/STOMP로 발행되는 협업 command event 메시지.
 */
public record CollaborationCommandEventMessage(
	Long commandEventId,
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
	Instant createdAt
) {
}
