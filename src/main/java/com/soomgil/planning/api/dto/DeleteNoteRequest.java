package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 메모 삭제 요청.
 *
 * @param baseVersion 클라이언트가 마지막으로 읽은 메모 버전
 */
public record DeleteNoteRequest(
	@NotNull
	@Positive
	Long baseVersion
) {
}
