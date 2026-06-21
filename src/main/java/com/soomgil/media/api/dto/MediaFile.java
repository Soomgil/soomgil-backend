package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 검증을 마친 media file의 공개 가능한 metadata 응답. */
public record MediaFile(
	@NotNull
	UUID id,
	URI publicUrl,
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
