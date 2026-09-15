package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import com.soomgil.community.application.command.UnlikeCommunityThreadCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadLikeMapper;
import com.soomgil.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UnlikeCommunityThreadCommand}를 처리해 쓰레드 좋아요를 취소한다.
 *
 * <p>좋아요가 없는 상태에서 호출해도 실패하지 않는 멱등 연산이며 항상 {@code liked=false}를 반환한다.
 */
@Component
@Transactional
public class UnlikeCommunityThreadCommandHandler
	implements CommandHandler<UnlikeCommunityThreadCommand, CommunityThreadReactionSummary> {

	private final CommunityThreadMapper threadMapper;
	private final ThreadLikeMapper threadLikeMapper;

	public UnlikeCommunityThreadCommandHandler(
		CommunityThreadMapper threadMapper,
		ThreadLikeMapper threadLikeMapper
	) {
		this.threadMapper = threadMapper;
		this.threadLikeMapper = threadLikeMapper;
	}

	@Override
	public CommunityThreadReactionSummary handle(UnlikeCommunityThreadCommand command) {
		threadMapper.findById(command.threadId())
			.filter(CommunityThreadRecord::isPubliclyReadable)
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));

		threadLikeMapper.delete(command.threadId(), command.actorUserId());
		return new CommunityThreadReactionSummary(
			command.threadId(),
			false,
			threadLikeMapper.countByThreadId(command.threadId())
		);
	}
}
