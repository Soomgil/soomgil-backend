package com.soomgil.global.storage;

import java.net.URI;
import java.util.Objects;

/**
 * S3 호환 storage에 저장된 object의 공통 metadata.
 *
 * <p>{@code publicUrl}은 공개 노출이 확정된 object에만 설정한다.
 * 검수 전 수상작 사진, private media, 임시 upload object는 {@code null}로 둔다.
 */
public record StorageObjectMetadata(
	String bucket,
	StorageObjectKey objectKey,
	String contentType,
	long sizeBytes,
	String checksumSha256,
	URI publicUrl
) {

	public StorageObjectMetadata {
		bucket = requireNotBlank(bucket, "bucket");
		Objects.requireNonNull(objectKey, "objectKey must not be null");
		contentType = requireNotBlank(contentType, "contentType");
		if (sizeBytes < 0) {
			throw new IllegalArgumentException("sizeBytes must be greater than or equal to 0");
		}
	}

	private static String requireNotBlank(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value;
	}
}
