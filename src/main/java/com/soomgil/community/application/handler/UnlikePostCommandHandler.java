package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityPostReactionSummary;
import com.soomgil.community.application.command.UnlikePostCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostLikeMapper;
import com.soomgil.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 좋아요를 취소한다.
 *
 * <p>좋아요하지 않은 상태에서 호출해도 에러 없이 처리된다(idempotent).
 */
@Component
@Transactional
public class UnlikePostCommandHandler
	implements CommandHandler<UnlikePostCommand, CommunityPostReactionSummary> {

	private final CommunityPostMapper postMapper;
	private final PostLikeMapper postLikeMapper;

	public UnlikePostCommandHandler(CommunityPostMapper postMapper, PostLikeMapper postLikeMapper) {
		this.postMapper = postMapper;
		this.postLikeMapper = postLikeMapper;
	}

	@Override
	public CommunityPostReactionSummary handle(UnlikePostCommand command) {
		CommunityPostRecord post = postMapper.findById(command.postId())
			.filter(p -> !p.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		postLikeMapper.delete(command.postId(), command.actorUserId());
		int likeCount = postLikeMapper.countByPostId(post.id());

		return new CommunityPostReactionSummary(post.id(), false, likeCount);
	}
}
