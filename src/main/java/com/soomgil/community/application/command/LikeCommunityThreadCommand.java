package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import java.util.UUID;

/**
 * 쓰레드 좋아요 command.
 *
 * <p>{@code (thread_id, user_id)} 복합 PK에 의해 멱등하다. 같은 사용자가 여러 번 호출해도
 * 좋아요는 1개만 유지되고 응답의 {@code likeCount}도 증가하지 않는다.
 *
 * @param threadId 대상 쓰레드 식별자
 * @param actorUserId 요청 사용자
 */
public record LikeCommunityThreadCommand(
	UUID threadId,
	UUID actorUserId
) implements Command<CommunityThreadReactionSummary> {
}
