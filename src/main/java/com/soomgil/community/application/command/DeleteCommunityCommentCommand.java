package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 커뮤니티 댓글 삭제 요청.
 *
 * @param postId 게시글 식별자
 * @param commentId 댓글 식별자
 * @param actorUserId 요청자 (댓글 작성자 본인이어야 함)
 */
public record DeleteCommunityCommentCommand(
	UUID postId,
	UUID commentId,
	UUID actorUserId
) implements Command<NoResult> {
}
