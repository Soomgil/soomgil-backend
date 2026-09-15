package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import java.util.UUID;

/**
 * 쓰레드 좋아요 취소 command.
 *
 * <p>좋아요가 없는 상태에서 호출해도 실패하지 않고 {@code liked=false}를 반환하는 멱등 연산이다.
 *
 * @param threadId 대상 쓰레드 식별자
 * @param actorUserId 요청 사용자
 */
public record UnlikeCommunityThreadCommand(
	UUID threadId,
	UUID actorUserId
) implements Command<CommunityThreadReactionSummary> {
}
