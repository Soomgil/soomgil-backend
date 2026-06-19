package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.CommunityPostReactionSummary;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.application.command.LikePostCommand;
import com.soomgil.community.application.command.UnlikePostCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostLikeMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LikePostCommandHandlerTest {

	private final CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
	private final PostLikeMapper postLikeMapper = mock(PostLikeMapper.class);

	private final LikePostCommandHandler likeHandler =
		new LikePostCommandHandler(postMapper, postLikeMapper);
	private final UnlikePostCommandHandler unlikeHandler =
		new UnlikePostCommandHandler(postMapper, postLikeMapper);

	@Test
	@DisplayName("좋아요 - 정상 처리 시 liked=true와 현재 좋아요 수를 반환한다")
	void likeReturnsSummaryWithTrue() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, userId)));
		when(postLikeMapper.countByPostId(postId)).thenReturn(3);

		CommunityPostReactionSummary result = likeHandler.handle(
			new LikePostCommand(postId, userId)
		);

		assertThat(result.liked()).isTrue();
		assertThat(result.likeCount()).isEqualTo(3);
		verify(postLikeMapper).insertOrIgnore(eq(postId), eq(userId), any(Instant.class));
	}

	@Test
	@DisplayName("좋아요 취소 - 정상 처리 시 liked=false와 감소된 좋아요 수를 반환한다")
	void unlikeReturnsSummaryWithFalse() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, userId)));
		when(postLikeMapper.countByPostId(postId)).thenReturn(1);

		CommunityPostReactionSummary result = unlikeHandler.handle(
			new UnlikePostCommand(postId, userId)
		);

		assertThat(result.liked()).isFalse();
		assertThat(result.likeCount()).isEqualTo(1);
		verify(postLikeMapper).delete(eq(postId), eq(userId));
	}

	@Test
	@DisplayName("삭제된 게시글에 좋아요 시도 시 POST_NOT_FOUND를 반환한다")
	void likeRejectsDeletedPost() {
		UUID postId = UUID.randomUUID();
		when(postMapper.findById(postId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> likeHandler.handle(new LikePostCommand(postId, UUID.randomUUID())))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.POST_NOT_FOUND));
	}

	private CommunityPostRecord samplePost(UUID postId, UUID userId) {
		return new CommunityPostRecord(
			postId, UUID.randomUUID(), 1L, userId,
			PostVisibility.PUBLIC, "title", "summary", null, 1,
			null, null, null, ModerationStatus.VISIBLE, Instant.now(), null
		);
	}
}
