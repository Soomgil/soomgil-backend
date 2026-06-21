package com.soomgil.global.storage;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/** 클라이언트가 object storage로 직접 업로드할 때 필요한 서명 정보. */
public record PresignedStorageUpload(
	URI uploadUrl,
	String method,
	Map<String, String> headers,
	OffsetDateTime expiresAt
) {
	public PresignedStorageUpload {
		Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
		method = Objects.requireNonNull(method, "method must not be null");
		headers = headers == null ? Map.of() : Map.copyOf(headers);
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
	}
}
