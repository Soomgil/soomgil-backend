package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.application.command.CreateCommunityThreadReplyCommand;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.domain.policy.CommunityThreadPolicy;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateCommunityThreadReplyCommand}를 처리해 쓰레드에 답글을 작성한다.
 *
 * <p>중첩 깊이는 {@link CommunityThreadPolicy#REPLY_MAX_DEPTH}까지만 허용한다. 이미 1단계인 답글을
 * 부모로 지정하면 {@code THREAD_REPLY_DEPTH_EXCEEDED}로 거부하고, 다른 쓰레드의 답글을 부모로 지정하면
 * {@code VALIDATION_FAILED}로 거부한다.
 */
@Component
@Transactional
public class CreateCommunityThreadReplyCommandHandler
	implements CommandHandler<CreateCommunityThreadReplyCommand, CommunityThreadReply> {

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadReplyMapper replyMapper;
	private final CommunityThreadAssembler assembler;

	public CreateCommunityThreadReplyCommandHandler(
		CommunityThreadMapper threadMapper,
		CommunityThreadReplyMapper replyMapper,
		CommunityThreadAssembler assembler
	) {
		this.threadMapper = threadMapper;
		this.replyMapper = replyMapper;
		this.assembler = assembler;
	}

	@Override
	public CommunityThreadReply handle(CreateCommunityThreadReplyCommand command) {
		threadMapper.findById(command.threadId())
			.filter(record -> !record.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));
		if (!CommunityThreadPolicy.isValidContent(command.content())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}

		int depth = resolveDepth(command);
		Instant now = Instant.now();
		UUID replyId = UUID.randomUUID();
		replyMapper.insert(
			replyId,
			command.threadId(),
			command.parentReplyId(),
			command.actorUserId(),
			command.content().strip(),
			depth,
			now
		);

		CommunityThreadReplyRecord saved = replyMapper.findById(replyId)
			.orElseThrow(() -> new CommunityException(ErrorCode.INTERNAL_ERROR));
		return assembler.toReply(saved, command.actorUserId());
	}

	private int resolveDepth(CreateCommunityThreadReplyCommand command) {
		if (command.parentReplyId() == null) {
			return 0;
		}
		CommunityThreadReplyRecord parent = replyMapper.findById(command.parentReplyId())
			.filter(record -> !record.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_REPLY_NOT_FOUND));
		if (!parent.threadId().equals(command.threadId())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (!CommunityThreadPolicy.canReplyTo(parent.depth())) {
			throw new CommunityException(ErrorCode.THREAD_REPLY_DEPTH_EXCEEDED);
		}
		return parent.depth() + 1;
	}
}
