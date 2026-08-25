package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import com.soomgil.community.application.command.LikeCommunityThreadCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadLikeMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LikeCommunityThreadCommand}를 처리해 쓰레드에 좋아요를 남긴다.
 *
 * <p>좋아요 테이블의 복합 PK와 ON CONFLICT DO NOTHING 덕분에 같은 요청을 반복해도 결과가 같다.
 * 삭제되거나 숨김 처리된 쓰레드에는 좋아요를 남길 수 없고 {@code THREAD_NOT_FOUND}로 응답한다.
 */
@Component
@Transactional
public class LikeCommunityThreadCommandHandler
	implements CommandHandler<LikeCommunityThreadCommand, CommunityThreadReactionSummary> {

	private final CommunityThreadMapper threadMapper;
	private final ThreadLikeMapper threadLikeMapper;

	public LikeCommunityThreadCommandHandler(
		CommunityThreadMapper threadMapper,
		ThreadLikeMapper threadLikeMapper
	) {
		this.threadMapper = threadMapper;
		this.threadLikeMapper = threadLikeMapper;
	}

	@Override
	public CommunityThreadReactionSummary handle(LikeCommunityThreadCommand command) {
		threadMapper.findById(command.threadId())
			.filter(CommunityThreadRecord::isPubliclyReadable)
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));

		threadLikeMapper.insertIfAbsent(command.threadId(), command.actorUserId(), Instant.now());
		return new CommunityThreadReactionSummary(
			command.threadId(),
			true,
			threadLikeMapper.countByThreadId(command.threadId())
		);
	}
}
