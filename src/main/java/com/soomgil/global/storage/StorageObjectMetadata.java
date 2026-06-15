package com.soomgil.global.storage;

import java.net.URI;
import java.util.Objects;

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
