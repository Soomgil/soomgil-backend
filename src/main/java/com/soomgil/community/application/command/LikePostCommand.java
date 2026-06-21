package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityPostReactionSummary;
import java.util.UUID;

/**
 * 게시글 좋아요 요청.
 *
 * @param postId 게시글 식별자
 * @param actorUserId 요청자
 */
public record LikePostCommand(
	UUID postId,
	UUID actorUserId
) implements Command<CommunityPostReactionSummary> {
}
