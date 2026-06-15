package com.soomgil.common.api.dto;

import java.util.List;

/**
 * page/size 기반 목록 응답의 공통 metadata.
 *
 * <p>{@code page}는 Spring Data와 같은 0 기반 page 번호를 사용한다.
 * {@code sort}는 {@code property,direction} 문자열 목록이며, 정렬이 없으면 빈 목록이다.
 */
public record PageMeta(
	Integer page,
	Integer size,
	Long totalElements,
	Integer totalPages,
	List<String> sort
) {
}
