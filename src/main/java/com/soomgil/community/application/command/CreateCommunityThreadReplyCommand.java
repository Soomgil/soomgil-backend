package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityThreadReply;
import java.util.UUID;

/**
 * 쓰레드 답글 작성 command.
 *
 * <p>{@code parentReplyId}가 null이면 depth 0, 값이 있으면 depth 1로 저장된다.
 * 부모가 이미 depth 1이면 {@code THREAD_REPLY_DEPTH_EXCEEDED}로 거부한다.
 *
 * @param threadId 대상 쓰레드 식별자
 * @param actorUserId 작성자
 * @param parentReplyId 부모 답글 식별자. root 답글이면 null
 * @param content 본문
 */
public record CreateCommunityThreadReplyCommand(
	UUID threadId,
	UUID actorUserId,
	UUID parentReplyId,
	String content
) implements Command<CommunityThreadReply> {
}
