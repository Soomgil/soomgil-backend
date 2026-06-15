package com.soomgil.global.storage;

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
