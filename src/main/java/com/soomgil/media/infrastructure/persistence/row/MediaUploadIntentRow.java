package com.soomgil.media.infrastructure.persistence.row;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 미디어 직접 업로드의 정리 상태. */
public record MediaUploadIntentRow(
	UUID id,
	UUID ownerUserId,
	String objectKey,
	String status,
	UUID mediaFileId,
	OffsetDateTime expiresAt,
	OffsetDateTime createdAt,
	OffsetDateTime completedAt
) {
}
