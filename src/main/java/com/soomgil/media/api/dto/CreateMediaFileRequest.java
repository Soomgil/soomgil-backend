package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record CreateMediaFileRequest(
	@NotBlank
	String objectKey,
	URI publicUrl,
	@NotBlank
	String mimeType,
	@NotNull
	Long byteSize,
	Integer width,
	Integer height,
	String linkedResourceType,
	UUID linkedResourceId
) {
}
