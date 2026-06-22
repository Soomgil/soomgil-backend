package com.soomgil.record.api.dto;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행 기록 사진의 현재 읽기 URL.
 */
public record TripRecordPhotoReadUrl(
	@NotNull UUID mediaFileId,
	@NotNull URI url,
	OffsetDateTime expiresAt
) {
}
