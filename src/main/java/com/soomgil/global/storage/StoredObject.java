package com.soomgil.global.storage;

import java.util.Objects;

/**
 * 저장소 metadata와 실제 binary 검사 결과.
 *
 * <p>{@code detectedContentType}과 이미지 크기는 object 내용을 읽어 서버가 판별한 값이다.
 */
public record StoredObject(
	StorageObjectMetadata metadata,
	String detectedContentType,
	Integer width,
	Integer height
) {
	public StoredObject {
		Objects.requireNonNull(metadata, "metadata must not be null");
	}
}
