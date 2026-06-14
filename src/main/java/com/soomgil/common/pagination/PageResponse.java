package com.soomgil.common.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
	List<T> items,
	PageMetadata page
) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
			page.getContent(),
			new PageMetadata(
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
			)
		);
	}

	public record PageMetadata(
		int page,
		int size,
		long totalElements,
		int totalPages
	) {
	}
}
