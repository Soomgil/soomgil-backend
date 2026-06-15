package com.soomgil.common.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * offset/limit 기반 목록 응답의 page metadata.
 *
 * <p>무한 스크롤이나 외부 source import처럼 전체 개수를 계산하지 않는 목록에서 사용한다.
 * {@code nextOffset}은 다음 page가 없으면 {@code null}일 수 있다.
 */
public record OffsetPageMeta(
	@NotNull
	@Min(0)
	Integer offset,
	@NotNull
	@Min(1)
	Integer limit,
	@Min(0)
	Integer nextOffset,
	@NotNull
	Boolean hasMore,
	List<String> sort
) {
}
