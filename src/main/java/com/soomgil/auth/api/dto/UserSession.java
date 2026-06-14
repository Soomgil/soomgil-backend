package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSession(
	@NotNull
	UUID id,
	@NotNull
	UUID refreshTokenFamilyId,
	@NotNull
	Integer refreshTokenVersion,
	String deviceName,
	String deviceOs,
	OffsetDateTime lastUsedAt,
	OffsetDateTime lastRefreshedAt,
	@NotNull
	OffsetDateTime expiresAt,
	OffsetDateTime revokedAt,
	String revocationReason
) {
}
