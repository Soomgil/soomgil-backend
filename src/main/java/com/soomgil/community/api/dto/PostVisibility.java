package com.soomgil.community.api.dto;

/**
 * 게시글 공개 범위.
 *
 * <p>PUBLIC은 누구나 접근 가능한 공개 게시글, UNLISTED는 shareToken을 통해서만
 * 접근할 수 있는 비공개 게시글을 나타낸다.
 */
public enum PostVisibility {
	PUBLIC,
	UNLISTED
}
