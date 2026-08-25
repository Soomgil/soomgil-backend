package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * 쓰레드 답글 삭제 command.
 *
 * <p>답글 작성자와 쓰레드 작성자가 삭제할 수 있다. 기존 게시글 댓글 정책과 같은 방향이며
 * soft delete로 처리해 tombstone을 남긴다.
 *
 * @param threadId 경로 검증용 쓰레드 식별자
 * @param replyId 삭제할 답글 식별자
 * @param actorUserId 요청 사용자
 */
public record DeleteCommunityThreadReplyCommand(
	UUID threadId,
	UUID replyId,
	UUID actorUserId
) implements Command<Void> {
}
