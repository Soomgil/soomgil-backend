package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.application.command.DeleteCommunityThreadReplyCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteCommunityThreadReplyCommand}를 처리해 답글을 soft delete한다.
 *
 * <p>답글 작성자와 쓰레드 작성자가 삭제할 수 있다. 기존 게시글 댓글 정책과 같은 방향이며,
 * 그 외 사용자는 {@code THREAD_AUTHOR_REQUIRED}로 거부한다. 삭제된 답글은 tombstone으로 남는다.
 */
@Component
@Transactional
public class DeleteCommunityThreadReplyCommandHandler
	implements CommandHandler<DeleteCommunityThreadReplyCommand, Void> {

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadReplyMapper replyMapper;

	public DeleteCommunityThreadReplyCommandHandler(
		CommunityThreadMapper threadMapper,
		CommunityThreadReplyMapper replyMapper
	) {
		this.threadMapper = threadMapper;
		this.replyMapper = replyMapper;
	}

	@Override
	public Void handle(DeleteCommunityThreadReplyCommand command) {
		CommunityThreadReplyRecord reply = replyMapper.findById(command.replyId())
			.filter(record -> !record.isDeleted())
			.filter(record -> record.threadId().equals(command.threadId()))
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_REPLY_NOT_FOUND));
		if (!canDelete(reply, command.threadId(), command.actorUserId())) {
			throw new CommunityException(ErrorCode.THREAD_AUTHOR_REQUIRED);
		}

		replyMapper.softDelete(command.replyId(), command.actorUserId(), Instant.now());
		return null;
	}

	private boolean canDelete(CommunityThreadReplyRecord reply, UUID threadId, UUID actorUserId) {
		if (reply.isAuthoredBy(actorUserId)) {
			return true;
		}
		return threadMapper.findById(threadId)
			.map(thread -> thread.isAuthoredBy(actorUserId))
			.orElse(false);
	}
}
