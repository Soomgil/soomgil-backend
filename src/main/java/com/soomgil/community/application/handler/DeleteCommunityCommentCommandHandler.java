package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.community.application.command.DeleteCommunityCommentCommand;
import com.soomgil.community.domain.model.CommunityCommentRecord;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityCommentMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 댓글을 soft delete한다.
 *
 * <p>댓글 작성자 본인만 삭제할 수 있다. moderator/admin 권한은 Phase 3에서 추가한다.
 * 이미 삭제된 댓글은 idempotent하게 처리한다.
 */
@Component
@Transactional
public class DeleteCommunityCommentCommandHandler
	implements CommandHandler<DeleteCommunityCommentCommand, NoResult> {

	private final CommunityCommentMapper commentMapper;

	public DeleteCommunityCommentCommandHandler(CommunityCommentMapper commentMapper) {
		this.commentMapper = commentMapper;
	}

	@Override
	public NoResult handle(DeleteCommunityCommentCommand command) {
		CommunityCommentRecord comment = commentMapper.findById(command.commentId())
			.orElseThrow(() -> new CommunityException(ErrorCode.RESOURCE_NOT_FOUND));

		if (comment.isDeleted()) {
			return NoResult.INSTANCE;
		}

		if (!comment.postId().equals(command.postId())) {
			throw new CommunityException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		if (!comment.isPublishedBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.FORBIDDEN);
		}

		commentMapper.softDelete(command.commentId(), null, Instant.now());
		return NoResult.INSTANCE;
	}
}
