package com.soomgil.global.storage;

import java.time.Duration;
import java.util.Objects;

/** S3 호환 저장소의 제한 시간 GET URL 발급 요청. */
public record StorageReadRequest(
	StorageObjectKey objectKey,
	Duration validity
) {
	public StorageReadRequest {
		Objects.requireNonNull(objectKey, "objectKey must not be null");
		Objects.requireNonNull(validity, "validity must not be null");
		if (validity.isZero() || validity.isNegative()) {
			throw new IllegalArgumentException("validity must be positive");
		}
	}
}
