package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityPostReactionSummary;
import com.soomgil.community.application.command.LikePostCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostLikeMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글에 좋아요를 표시한다.
 *
 * <p>이미 좋아요한 경우 idempotent하게 처리된다(ON CONFLICT DO NOTHING).
 */
@Component
@Transactional
public class LikePostCommandHandler
	implements CommandHandler<LikePostCommand, CommunityPostReactionSummary> {

	private final CommunityPostMapper postMapper;
	private final PostLikeMapper postLikeMapper;

	public LikePostCommandHandler(CommunityPostMapper postMapper, PostLikeMapper postLikeMapper) {
		this.postMapper = postMapper;
		this.postLikeMapper = postLikeMapper;
	}

	@Override
	public CommunityPostReactionSummary handle(LikePostCommand command) {
		CommunityPostRecord post = postMapper.findById(command.postId())
			.filter(p -> !p.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		postLikeMapper.insertOrIgnore(command.postId(), command.actorUserId(), Instant.now());
		int likeCount = postLikeMapper.countByPostId(post.id());

		return new CommunityPostReactionSummary(post.id(), true, likeCount);
	}
}
