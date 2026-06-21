package com.soomgil.ai.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

public record AiChatMessageRow(
	UUID id,
	UUID sessionId,
	UUID requesterUserId,
	String role,
	String content,
	UUID toolCallId,
	Instant createdAt,
	String requesterDisplayName,
	String requesterProfileImageUrl
) {
}
