package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 삭제 command.
 *
 * <p>작성자만 삭제할 수 있고 hard delete가 아니라 soft delete로 처리한다.
 * 삭제된 쓰레드는 tombstone으로 남고 본문은 더 이상 응답에 포함되지 않는다.
 *
 * @param threadId 삭제할 쓰레드 식별자
 * @param actorUserId 요청 사용자
 */
public record DeleteCommunityThreadCommand(
	UUID threadId,
	UUID actorUserId
) implements Command<Void> {
}
