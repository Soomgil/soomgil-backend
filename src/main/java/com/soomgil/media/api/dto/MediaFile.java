package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 검증을 마친 media file의 표시용 metadata 응답.
 *
 * <p>공개 media는 {@code publicUrl}을 사용한다. 비공개 media는 권한 확인 후 제한 시간 동안 유효한
 * {@code servingUrl}과 {@code servingUrlExpiresAt}을 함께 반환한다.
 */
public record MediaFile(
	@NotNull
	UUID id,
	URI publicUrl,
	URI servingUrl,
	OffsetDateTime servingUrlExpiresAt,
	@NotBlank
	String mimeType,
	Long byteSize,
	Integer width,
	Integer height,
	@NotBlank
	String status,
	@NotNull
	OffsetDateTime createdAt
) {
}
