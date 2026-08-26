package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 한 번의 지우개 제스처로 선택된 map drawing 일괄 삭제 요청.
 *
 * <p>{@code drawingIds}는 중복을 제외해 최대 100개까지 처리하며, 전체 삭제는 하나의
 * collaboration command event로 기록되어 한 번의 undo/redo 대상으로 취급된다.
 */
public record DeleteMapDrawingsRequest(
	@NotNull
	Long baseVersion,
	@NotEmpty
	@Size(max = 100)
	List<@NotNull UUID> drawingIds
) {
}
