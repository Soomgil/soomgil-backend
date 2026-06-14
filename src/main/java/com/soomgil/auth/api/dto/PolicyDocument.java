package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyDocument(
	@NotNull
	UUID id,
	@NotBlank
	String policyCode,
	@NotBlank
	String version,
	@NotBlank
	String languageCode,
	@NotBlank
	String title,
	URI contentUrl,
	String contentHash,
	@NotNull
	Boolean isRequired,
	@NotNull
	OffsetDateTime publishedAt
) {
}
