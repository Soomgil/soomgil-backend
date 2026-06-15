package com.soomgil.common.pagination;

import com.soomgil.common.api.dto.PageMeta;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public record PageResponse<T>(
	List<T> items,
	PageMeta page
) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
			page.getContent(),
			new PageMeta(
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				sortFields(page.getSort())
			)
		);
	}

	private static List<String> sortFields(Sort sort) {
		return sort.stream()
			.map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
			.toList();
	}
}
