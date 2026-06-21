package com.soomgil.chat.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

/** 채팅 메시지와 발신자 공개 정보를 합친 읽기 row. */
public record TripChatMessageRow(
	UUID id,
	UUID tripId,
	UUID senderUserId,
	String senderDisplayName,
	String senderProfileImageUrl,
	String content,
	Instant deletedAt,
	Instant createdAt
) {
}
