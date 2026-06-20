package com.soomgil.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.UUID;

/** 업로드 완료 object를 검증해 media metadata로 등록하기 위한 요청. */
public record CreateMediaFileRequest(
	@NotBlank
	String objectKey,
	URI publicUrl,
	@NotBlank
	String mimeType,
	@NotNull
	@Positive
	Long byteSize,
	@Positive
	Integer width,
	@Positive
	Integer height,
	String linkedResourceType,
	UUID linkedResourceId
) {
}
