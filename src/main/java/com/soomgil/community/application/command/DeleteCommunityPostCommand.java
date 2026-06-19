package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 커뮤니티 게시글 삭제 요청.
 *
 * @param postId 게시글 식별자
 * @param actorUserId 요청자 (발행자 본인 또는 moderator/admin)
 * @param reason 삭제 사유 (nullable)
 */
public record DeleteCommunityPostCommand(
	UUID postId,
	UUID actorUserId,
	String reason
) implements Command<NoResult> {
}
