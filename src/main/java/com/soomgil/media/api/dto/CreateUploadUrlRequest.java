package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUploadUrlRequest(
	@NotBlank
	String fileName,
	@NotBlank
	String mimeType,
	@NotNull
	Long byteSize,
	String purpose
) {
}
