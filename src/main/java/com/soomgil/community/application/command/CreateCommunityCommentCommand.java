package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityComment;
import java.util.UUID;

/**
 * 커뮤니티 댓글 작성 요청.
 *
 * @param postId 게시글 식별자
 * @param actorUserId 작성자
 * @param parentCommentId 부모 댓글 (대댓글, nullable)
 * @param content 본문
 */
public record CreateCommunityCommentCommand(
	UUID postId,
	UUID actorUserId,
	UUID parentCommentId,
	String content
) implements Command<CommunityComment> {
}
