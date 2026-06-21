package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.community.application.command.DeleteCommunityPostCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글을 soft delete한다.
 *
 * <p>발행자 본인만 삭제할 수 있다. moderator/admin 권한은 Phase 3 모더레이션에서 추가한다.
 * 이미 삭제된 게시글은 idempotent하게 처리한다.
 */
@Component
@Transactional
public class DeleteCommunityPostCommandHandler
	implements CommandHandler<DeleteCommunityPostCommand, NoResult> {

	private final CommunityPostMapper postMapper;

	public DeleteCommunityPostCommandHandler(CommunityPostMapper postMapper) {
		this.postMapper = postMapper;
	}

	@Override
	public NoResult handle(DeleteCommunityPostCommand command) {
		CommunityPostRecord post = postMapper.findById(command.postId())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		if (post.isDeleted()) {
			return NoResult.INSTANCE;
		}

		if (!post.isPublishedBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.POST_AUTHOR_REQUIRED);
		}

		postMapper.softDelete(command.postId(), command.reason(), Instant.now());
		return NoResult.INSTANCE;
	}
}
