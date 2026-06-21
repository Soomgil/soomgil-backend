package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 커뮤니티 게시글에 달린 댓글 응답.
 *
 * <p>대댓글은 {@code parentCommentId}로 부모 댓글을 참조하며, {@code depth}로 계층 깊이를 표현한다.
 * {@code moderationStatus}가 HIDDEN 또는 DELETED인 댓글은 공개 목록에서 제외된다.
 * soft delete된 댓글은 {@code deletedAt}만 채워지고 식별자는 유지된다.
 *
 * @param id 댓글 식별자
 * @param postId 댓글이 속한 게시글 식별자
 * @param parentCommentId 대댓글인 경우 부모 댓글 식별자, 최상위 댓글이면 null
 * @param author 댓글 작성자 요약 정보
 * @param content 댓글 본문
 * @param depth 댓글 계층 깊이 (최상위 0)
 * @param moderationStatus 모더레이션 상태 (VISIBLE, HIDDEN, DELETED)
 * @param deletedAt soft delete 시각, 삭제되지 않았으면 null
 * @param createdAt 댓글 생성 시각
 */
public record CommunityComment(
	@NotNull
	UUID id,
	@NotNull
	UUID postId,
	UUID parentCommentId,
	@Valid
	@NotNull
	UserSummary author,
	String content,
	@NotNull
	Integer depth,
	@NotNull
	ModerationStatus moderationStatus,
	OffsetDateTime deletedAt,
	@NotNull
	OffsetDateTime createdAt
) {
}
