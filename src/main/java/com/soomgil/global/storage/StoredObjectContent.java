package com.soomgil.global.storage;

import java.util.Objects;

/**
 * 한 번 읽은 object의 검증 결과와 binary 내용.
 *
 * @param object storage metadata와 content 검사 결과
 * @param bytes object binary
 */
public record StoredObjectContent(StoredObject object, byte[] bytes) {
	public StoredObjectContent {
		Objects.requireNonNull(object, "object must not be null");
		Objects.requireNonNull(bytes, "bytes must not be null");
	}
}
