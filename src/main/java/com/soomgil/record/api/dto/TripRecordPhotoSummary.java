package com.soomgil.record.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.util.UUID;

/**
 * 한 여행의 기록 사진 개수와 대표 사진 요약.
 *
 * <p>사진이 없으면 {@code photoCount}는 0이고 {@code coverUrl}은 null이다.
 */
public record TripRecordPhotoSummary(
	@NotNull
	UUID tripId,
	@PositiveOrZero
	long photoCount,
	UUID coverMediaFileId,
	URI coverUrl,
	java.time.OffsetDateTime coverUrlExpiresAt
) {
}
