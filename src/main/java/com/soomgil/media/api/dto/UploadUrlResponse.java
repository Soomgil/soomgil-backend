package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

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
