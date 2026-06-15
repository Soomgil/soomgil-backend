package com.soomgil.common.pagination;

import com.soomgil.common.api.dto.PageMeta;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

/**
 * page 기반 목록 API의 공통 응답 형식.
 *
 * <p>{@code items}에는 현재 page의 데이터만 담고, {@code page}에는 0부터 시작하는 page 번호,
 * size, 전체 개수, 전체 page 수, 정렬 조건을 담는다.
 *
 * @param <T> 목록 item 타입
 */
public record PageResponse<T>(
	List<T> items,
	PageMeta page
) {

	/**
	 * Spring Data {@link Page}를 API 공통 page 응답으로 변환한다.
	 *
	 * @param page Spring Data page 결과
	 * @param <T> item 타입
	 * @return 공통 page 응답
	 */
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
