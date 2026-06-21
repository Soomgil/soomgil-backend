package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityComment;
import com.soomgil.community.application.command.CreateCommunityCommentCommand;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.domain.model.CommunityCommentRecord;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.domain.policy.CommunityPostPolicy;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityCommentMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글에 댓글을 작성한다.
 *
 * <p>대댓글은 {@code parentCommentId}를 지정하면 작성할 수 있으며, 깊이는
 * {@link CommunityPostPolicy#COMMENT_MAX_DEPTH}까지 허용된다.
 */
@Component
@Transactional
public class CreateCommunityCommentCommandHandler
	implements CommandHandler<CreateCommunityCommentCommand, CommunityComment> {

	private final CommunityPostMapper postMapper;
	private final CommunityCommentMapper commentMapper;
	private final CommunityPostAssembler assembler;

	public CreateCommunityCommentCommandHandler(
		CommunityPostMapper postMapper,
		CommunityCommentMapper commentMapper,
		CommunityPostAssembler assembler
	) {
		this.postMapper = postMapper;
		this.commentMapper = commentMapper;
		this.assembler = assembler;
	}

	@Override
	public CommunityComment handle(CreateCommunityCommentCommand command) {
		CommunityPostRecord post = postMapper.findById(command.postId())
			.filter(p -> !p.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		validateContent(command.content());

		int depth = 0;
		if (command.parentCommentId() != null) {
			CommunityCommentRecord parent = commentMapper.findById(command.parentCommentId())
				.orElseThrow(() -> new CommunityException(ErrorCode.RESOURCE_NOT_FOUND));
			if (parent.isDeleted()) {
				throw new CommunityException(ErrorCode.RESOURCE_NOT_FOUND);
			}
			if (!parent.postId().equals(command.postId())) {
				throw new CommunityException(ErrorCode.VALIDATION_FAILED);
			}
			depth = parent.depth() + 1;
			if (depth > CommunityPostPolicy.COMMENT_MAX_DEPTH) {
				throw new CommunityException(ErrorCode.VALIDATION_FAILED);
			}
		}

		Instant now = Instant.now();
		UUID commentId = UUID.randomUUID();
		commentMapper.insert(
			commentId, command.postId(), command.parentCommentId(),
			command.actorUserId(), command.content(), depth, now
		);

		CommunityCommentRecord saved = commentMapper.findById(commentId)
			.orElseThrow(() -> new CommunityException(ErrorCode.INTERNAL_ERROR));
		return assembler.toComment(saved);
	}

	private void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (content.length() > CommunityPostPolicy.COMMENT_CONTENT_MAX) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
	}
}
