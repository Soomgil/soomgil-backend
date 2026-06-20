package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

/** object storage에 직접 PUT할 URL과 필수 header 응답. */
public record UploadUrlResponse(
	@NotNull
	URI uploadUrl,
	String method,
	@NotBlank
	String objectKey,
	Map<String, String> headers,
	@NotNull
	OffsetDateTime expiresAt
) {
}
