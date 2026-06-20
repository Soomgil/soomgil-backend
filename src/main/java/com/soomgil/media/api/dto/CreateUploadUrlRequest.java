package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.soomgil.media.domain.model.MediaPurpose;

/** 목적별 제한을 적용해 직접 업로드 URL을 발급받기 위한 요청. */
public record CreateUploadUrlRequest(
	@NotBlank
	String fileName,
	@NotBlank
	String mimeType,
	@NotNull
	@Positive
	Long byteSize,
	@NotNull
	MediaPurpose purpose
) {
}
