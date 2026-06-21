package com.soomgil.media.infrastructure.persistence.row;

import java.time.OffsetDateTime;
import java.util.UUID;

/** media.media_files SQL row. */
public record MediaFileRow(
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
	OffsetDateTime createdAt,
	OffsetDateTime deletedAt,
	OffsetDateTime purgeAfterAt
) {
}
