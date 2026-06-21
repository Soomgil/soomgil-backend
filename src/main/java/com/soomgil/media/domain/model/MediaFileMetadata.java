package com.soomgil.media.domain.model;

import com.soomgil.global.storage.StorageObjectKey;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 검증된 object를 가리키는 media file metadata.
 *
 * <p>실제 binary는 object storage에 있고 이 model에는 소유권, 노출 정보, 삭제 상태만 저장한다.
 */
public record MediaFileMetadata(
	UUID id,
	UUID ownerUserId,
	String storageProvider,
	String bucket,
	StorageObjectKey objectKey,
	URI publicUrl,
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
	public MediaFileMetadata {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
		Objects.requireNonNull(objectKey, "objectKey must not be null");
	}
}
