package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.application.command.UpdateCommunityThreadReplyCommand;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.domain.policy.CommunityThreadPolicy;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateCommunityThreadReplyCommand}를 처리해 답글 본문을 수정한다.
 *
 * <p>답글 작성자만 수정할 수 있다. 쓰레드 작성자에게는 수정 권한이 없고 삭제 권한만 있다.
 */
@Component
@Transactional
public class UpdateCommunityThreadReplyCommandHandler
	implements CommandHandler<UpdateCommunityThreadReplyCommand, CommunityThreadReply> {

	private final CommunityThreadReplyMapper replyMapper;
	private final CommunityThreadAssembler assembler;

	public UpdateCommunityThreadReplyCommandHandler(
		CommunityThreadReplyMapper replyMapper,
		CommunityThreadAssembler assembler
	) {
		this.replyMapper = replyMapper;
		this.assembler = assembler;
	}

	@Override
	public CommunityThreadReply handle(UpdateCommunityThreadReplyCommand command) {
		CommunityThreadReplyRecord reply = replyMapper.findById(command.replyId())
			.filter(record -> !record.isDeleted())
			.filter(record -> record.threadId().equals(command.threadId()))
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_REPLY_NOT_FOUND));
		if (!reply.isAuthoredBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.THREAD_AUTHOR_REQUIRED);
		}
		if (!CommunityThreadPolicy.isValidContent(command.content())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}

		replyMapper.updateContent(command.replyId(), command.content().strip(), Instant.now());
		CommunityThreadReplyRecord updated = replyMapper.findById(command.replyId())
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_REPLY_NOT_FOUND));
		return assembler.toReply(updated, command.actorUserId());
	}
}
