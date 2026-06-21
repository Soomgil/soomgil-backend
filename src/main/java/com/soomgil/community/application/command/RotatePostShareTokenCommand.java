package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityPostShareTokenResponse;
import java.util.UUID;

/**
 * 게시글 공유 토큰 rotate 요청.
 *
 * @param postId 게시글 식별자
 * @param actorUserId 요청자 (발행자 본인)
 */
public record RotatePostShareTokenCommand(
	UUID postId,
	UUID actorUserId
) implements Command<CommunityPostShareTokenResponse> {
}
