package com.soomgil.community.domain.model;

import com.soomgil.community.api.dto.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 커뮤니티 게시글 댓글 row.
 *
 * @param id 댓글 식별자
 * @param postId 게시글 식별자
 * @param parentCommentId 부모 댓글 식별자 (대댓글, nullable)
 * @param authorUserId 작성자
 * @param content 본문
 * @param depth 중첩 깊이 (0 = 최상위)
 * @param moderationStatus 모더레이션 상태
 * @param deletedAt soft delete 시각 (null이면 활성)
 * @param createdAt 생성 시각
 */
public record CommunityCommentRecord(
	UUID id,
	UUID postId,
	UUID parentCommentId,
	UUID authorUserId,
	String content,
	int depth,
	ModerationStatus moderationStatus,
	Instant deletedAt,
	Instant createdAt
) {

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isPublishedBy(UUID userId) {
		return userId != null && userId.equals(authorUserId);
	}
}
