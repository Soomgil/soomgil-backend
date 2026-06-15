package com.soomgil.global.storage;

/**
 * S3/MinIO object key 값 객체.
 *
 * <p>object key는 bucket 내부의 상대 경로이며, 보안상 절대 경로, path traversal, backslash를 허용하지 않는다.
 * 예: {@code media/users/user-1/avatar.png}
 */
public record StorageObjectKey(String value) {

	public StorageObjectKey {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("storage object key must not be blank");
		}
		if (value.startsWith("/")) {
			throw new IllegalArgumentException("storage object key must be relative");
		}
		if (value.contains("..")) {
			throw new IllegalArgumentException("storage object key must not contain path traversal");
		}
		if (value.contains("\\")) {
			throw new IllegalArgumentException("storage object key must use forward slashes");
		}
	}
}
