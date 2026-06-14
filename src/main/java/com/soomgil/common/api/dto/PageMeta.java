package com.soomgil.common.api.dto;

import java.util.List;

public record PageMeta(
	Integer page,
	Integer size,
	Long totalElements,
	Integer totalPages,
	List<String> sort
) {
}
