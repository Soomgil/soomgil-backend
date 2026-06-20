package com.soomgil.global.storage;

import java.time.Duration;
import java.util.Objects;

/** S3 호환 저장소의 제한 시간 PUT URL 발급 요청. */
public record StorageUploadRequest(
	StorageObjectKey objectKey,
	String contentType,
	long byteSize,
	Duration validity
) {
	public StorageUploadRequest {
		Objects.requireNonNull(objectKey, "objectKey must not be null");
		if (contentType == null || contentType.isBlank()) {
			throw new IllegalArgumentException("contentType must not be blank");
		}
		if (byteSize <= 0) {
			throw new IllegalArgumentException("byteSize must be positive");
		}
		Objects.requireNonNull(validity, "validity must not be null");
	}
}
