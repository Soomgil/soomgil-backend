package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.application.command.DeleteCommunityThreadCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteCommunityThreadCommand}를 처리해 쓰레드를 soft delete한다.
 *
 * <p>작성자만 삭제할 수 있다. 삭제 후에도 답글과 좋아요 row는 남지만 본문은 응답에서 사라지고
 * 공개 피드에서도 제외된다.
 */
@Component
@Transactional
public class DeleteCommunityThreadCommandHandler
	implements CommandHandler<DeleteCommunityThreadCommand, Void> {

	private final CommunityThreadMapper threadMapper;

	public DeleteCommunityThreadCommandHandler(CommunityThreadMapper threadMapper) {
		this.threadMapper = threadMapper;
	}

	@Override
	public Void handle(DeleteCommunityThreadCommand command) {
		CommunityThreadRecord thread = threadMapper.findById(command.threadId())
			.filter(record -> !record.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));
		if (!thread.isAuthoredBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.THREAD_AUTHOR_REQUIRED);
		}

		threadMapper.softDelete(command.threadId(), command.actorUserId(), Instant.now());
		return null;
	}
}
