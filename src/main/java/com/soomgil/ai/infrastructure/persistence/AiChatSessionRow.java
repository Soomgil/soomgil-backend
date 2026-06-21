package com.soomgil.ai.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

public record AiChatSessionRow(
	UUID id,
	UUID tripId,
	String status,
	String summary,
	Instant summaryUpdatedAt,
	Instant createdAt
) {
}
