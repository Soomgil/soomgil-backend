package com.soomgil.media.application.command.dto;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

/** 클라이언트 직접 업로드에 필요한 URL, key, header와 만료 시각. */
public record UploadUrlView(
	URI uploadUrl,
	String method,
	String objectKey,
	Map<String, String> headers,
	OffsetDateTime expiresAt
) {
}
