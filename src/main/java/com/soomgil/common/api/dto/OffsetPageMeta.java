package com.soomgil.common.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

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
