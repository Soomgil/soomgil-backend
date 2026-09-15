package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.application.command.CreateCommunityThreadCommand;
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
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateCommunityThreadCommand}를 처리해 공개 피드에 쓰레드를 작성한다.
 *
 * <p>쓰기 handler이므로 transaction 경계를 가지며 쓰레드 row와 첨부 미디어 연결을 함께 저장한다.
 * 본문 길이 위반은 {@code VALIDATION_FAILED}, 이미지 개수 초과는 {@code THREAD_MEDIA_LIMIT_EXCEEDED},
 * 요청자 소유가 아닌 미디어는 {@code MEDIA_LINK_FORBIDDEN}으로 거부한다.
 */
@Component
@Transactional
public class CreateCommunityThreadCommandHandler
	implements CommandHandler<CreateCommunityThreadCommand, CommunityThread> {

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadAssembler assembler;
	private final CommunityThreadMediaLinker mediaLinker;

	public CreateCommunityThreadCommandHandler(
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
	public CommunityThread handle(CreateCommunityThreadCommand command) {
		mediaLinker.validateMediaCount(command.mediaFileIds());
		if (!CommunityThreadPolicy.isValidContent(command.content())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		mediaLinker.validateOwnership(command.mediaFileIds(), command.actorUserId());

		Instant now = Instant.now();
		UUID threadId = UUID.randomUUID();
		threadMapper.insert(threadId, command.actorUserId(), command.content().strip(), now);
		mediaLinker.link(threadId, command.mediaFileIds(), now);

		CommunityThreadRecord saved = threadMapper.findById(threadId)
			.orElseThrow(() -> new CommunityException(ErrorCode.INTERNAL_ERROR));
		return assembler.toThread(saved, command.actorUserId());
	}
}
