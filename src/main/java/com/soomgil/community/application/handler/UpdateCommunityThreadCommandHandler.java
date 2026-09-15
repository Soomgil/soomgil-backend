package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.application.command.UpdateCommunityThreadCommand;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.application.service.CommunityThreadMediaLinker;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.domain.policy.CommunityThreadPolicy;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadMediaMapper;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.application.MediaFileQueryService;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateCommunityThreadCommand}를 처리해 쓰레드 본문과 첨부 이미지를 수정한다.
 *
 * <p>작성자만 수정할 수 있다. 삭제된 쓰레드와 존재하지 않는 쓰레드는 모두 {@code THREAD_NOT_FOUND}로
 * 응답해 존재 여부를 은닉한다. {@code mediaFileIds}가 null이면 기존 첨부를 그대로 유지한다.
 */
@Component
@Transactional
public class UpdateCommunityThreadCommandHandler
	implements CommandHandler<UpdateCommunityThreadCommand, CommunityThread> {

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadAssembler assembler;
	private final CommunityThreadMediaLinker mediaLinker;

	public UpdateCommunityThreadCommandHandler(
		CommunityThreadMapper threadMapper,
		ThreadMediaMapper threadMediaMapper,
		MediaFileQueryService mediaFileQueryService,
		CommunityThreadAssembler assembler
	) {
		this.threadMapper = threadMapper;
		this.assembler = assembler;
		this.mediaLinker = new CommunityThreadMediaLinker(threadMediaMapper, mediaFileQueryService);
	}

	@Override
	public CommunityThread handle(UpdateCommunityThreadCommand command) {
		CommunityThreadRecord thread = threadMapper.findById(command.threadId())
			.filter(record -> !record.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));
		if (!thread.isAuthoredBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.THREAD_AUTHOR_REQUIRED);
		}

		mediaLinker.validateMediaCount(command.mediaFileIds());
		if (!CommunityThreadPolicy.isValidContent(command.content())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		mediaLinker.validateOwnership(command.mediaFileIds(), command.actorUserId());

		Instant now = Instant.now();
		threadMapper.updateContent(command.threadId(), command.content().strip(), now);
		if (command.mediaFileIds() != null) {
			mediaLinker.replace(command.threadId(), command.mediaFileIds(), now);
		}

		CommunityThreadRecord updated = threadMapper.findById(command.threadId())
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));
		return assembler.toThread(updated, command.actorUserId());
	}
}
