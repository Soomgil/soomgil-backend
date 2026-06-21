package com.soomgil.media.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

public record MediaFileRecord(
	UUID id,
	UUID ownerUserId,
	String storageProvider,
	String bucket,
	String objectKey,
	String publicUrl,
	String mimeType,
	Long byteSize,
	Integer width,
	Integer height,
	String linkedResourceType,
	UUID linkedResourceId,
	String status,
	Instant createdAt
) {
}
