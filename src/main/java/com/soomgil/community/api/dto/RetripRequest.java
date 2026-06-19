package com.soomgil.community.api.dto;

/**
 * 게시글 retrip(재여행) 요청.
 *
 * <p>다른 사용자의 게시글 snapshot을 자신의 새 여행방으로 복사할 때 사용한다.
 * {@code title}을 생략하면 원본 게시글 제목이 그대로 사용된다.
 *
 * @param title 새 여행방에 사용할 제목, 생략 시 원본 제목 사용
 */
public record RetripRequest(
	String title
) {
}
