package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.PagedCommunityComment;
import java.util.UUID;

/**
 * 커뮤니티 댓글 목록 조회 요청.
 *
 * @param postId 게시글 식별자
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기
 */
public record ListCommentsQuery(
	UUID postId,
	int page,
	int size
) implements Query<PagedCommunityComment> {
}
