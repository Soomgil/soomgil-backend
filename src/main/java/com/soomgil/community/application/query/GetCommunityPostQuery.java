package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.CommunityPostDetail;
import java.util.UUID;

/**
 * 커뮤니티 게시글 상세 조회 요청.
 *
 * @param postId 게시글 식별자
 * @param viewerUserId 조회자 (null이면 비로그인)
 * @param shareToken UNLISTED 비작성자 접근 시 필요한 raw 공유 토큰 (nullable)
 */
public record GetCommunityPostQuery(
	UUID postId,
	UUID viewerUserId,
	String shareToken
) implements Query<CommunityPostDetail> {
}
