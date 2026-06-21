package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.PagedCommunityPostSummary;
import com.soomgil.community.api.dto.PostVisibility;
import java.util.UUID;

/**
 * 커뮤니티 게시글 목록 조회 요청.
 *
 * <p>MVP에서는 공개 feed(visibility=PUBLIC만)와 특정 사용자 발행 목록만 지원한다.
 *
 * @param publisherUserId 특정 사용자 발행 목록 (null이면 공개 feed)
 * @param visibility 공개 범위 필터 (현재는 PUBLIC만 허용)
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기
 * @param viewerUserId 조회하는 사용자 식별자 (nullable, likedByMe 등 계산용)
 */
public record ListCommunityPostsQuery(
	UUID publisherUserId,
	PostVisibility visibility,
	int page,
	int size,
	UUID viewerUserId
) implements Query<PagedCommunityPostSummary> {
}
