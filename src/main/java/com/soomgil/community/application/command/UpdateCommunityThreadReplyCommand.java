package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityThreadReply;
import java.util.UUID;

/**
 * 쓰레드 답글 수정 command. 답글 작성자만 수정할 수 있다.
 *
 * @param threadId 경로 검증용 쓰레드 식별자
 * @param replyId 수정할 답글 식별자
 * @param actorUserId 요청 사용자
 * @param content 새 본문
 */
public record UpdateCommunityThreadReplyCommand(
	UUID threadId,
	UUID replyId,
	UUID actorUserId,
	String content
) implements Command<CommunityThreadReply> {
}
