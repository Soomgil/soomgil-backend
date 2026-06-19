package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 커뮤니티 댓글 생성 요청.
 *
 * <p>{@code parentCommentId}가 있으면 대댓글로 생성된다.
 * {@code content}는 1자 이상 2000자 이하여야 한다.
 *
 * @param parentCommentId 부모 댓글 식별자, 최상위 댓글이면 null
 * @param content 댓글 본문 (1~2000자)
 */
public record CreateCommunityCommentRequest(
	UUID parentCommentId,
	@NotBlank
	@Size(min = 1, max = 2000)
	String content
) {
}
