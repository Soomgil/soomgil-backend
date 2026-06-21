package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.application.query.GetCommunityPostQuery;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.application.service.ShareTokenService;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetCommunityPostQueryHandlerTest {

	private final CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
	private final ShareTokenService shareTokenService = mock(ShareTokenService.class);
	private final CommunityPostAssembler assembler = mock(CommunityPostAssembler.class);

	private final GetCommunityPostQueryHandler handler = new GetCommunityPostQueryHandler(
		postMapper, shareTokenService, assembler
	);

	@Test
	@DisplayName("PUBLIC 게시글은 비로그인도 조회할 수 있다")
	void returnsPublicPostForAnyone() {
		UUID postId = UUID.randomUUID();
		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, UUID.randomUUID(), PostVisibility.PUBLIC)));
		when(assembler.toDetail(any(), any(), any(), eq(true)))
			.thenReturn(sampleDetail(postId));

		CommunityPostDetail result = handler.handle(
			new GetCommunityPostQuery(postId, null, null)
		);

		assertThat(result).isNotNull();
	}

	@Test
	@DisplayName("UNLISTED 게시글은 토큰 없이 비로그인 접근 시 POST_NOT_FOUND로 존재를 숨긴다")
	void hidesUnlistedWithoutToken() {
		UUID postId = UUID.randomUUID();
		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, UUID.randomUUID(), PostVisibility.UNLISTED)));

		assertThatThrownBy(() -> handler.handle(
			new GetCommunityPostQuery(postId, null, null)
		))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.POST_NOT_FOUND));
	}

	@Test
	@DisplayName("UNLISTED 게시글은 유효한 share token으로 조회할 수 있다")
	void returnsUnlistedWithValidToken() {
		UUID postId = UUID.randomUUID();
		CommunityPostRecord post = samplePost(postId, UUID.randomUUID(), PostVisibility.UNLISTED);

		when(postMapper.findById(postId)).thenReturn(Optional.of(post));
		when(shareTokenService.hash("raw-token")).thenReturn(post.shareTokenHash());
		when(assembler.toDetail(any(), any(), any(), eq(true)))
			.thenReturn(sampleDetail(postId));

		CommunityPostDetail result = handler.handle(
			new GetCommunityPostQuery(postId, null, "raw-token")
		);

		assertThat(result).isNotNull();
		verify(shareTokenService).hash("raw-token");
	}

	@Test
	@DisplayName("UNLISTED 게시글에 잘못된 token을 제공하면 INVALID_SHARE_TOKEN을 반환한다")
	void rejectsInvalidToken() {
		UUID postId = UUID.randomUUID();
		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, UUID.randomUUID(), PostVisibility.UNLISTED)));
		when(shareTokenService.hash("wrong-token")).thenReturn("wrong-hash");

		assertThatThrownBy(() -> handler.handle(
			new GetCommunityPostQuery(postId, null, "wrong-token")
		))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.INVALID_SHARE_TOKEN));
	}

	@Test
	@DisplayName("삭제된 게시글은 발행자 본인만 조회할 수 있다")
	void hidesDeletedPostFromNonPublisher() {
		UUID postId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();
		when(postMapper.findById(postId))
			.thenReturn(Optional.of(deletedPost(postId, publisherId, PostVisibility.PUBLIC)));

		assertThatThrownBy(() -> handler.handle(
			new GetCommunityPostQuery(postId, UUID.randomUUID(), null)
		))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.POST_NOT_FOUND));
	}

	@Test
	@DisplayName("삭제된 게시글을 발행자가 조회하면 정상 반환한다")
	void returnsDeletedPostForPublisher() {
		UUID postId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();
		when(postMapper.findById(postId))
			.thenReturn(Optional.of(deletedPost(postId, publisherId, PostVisibility.PUBLIC)));
		when(assembler.toDetail(any(), eq(publisherId), any(), eq(true)))
			.thenReturn(sampleDetail(postId));

		CommunityPostDetail result = handler.handle(
			new GetCommunityPostQuery(postId, publisherId, null)
		);

		assertThat(result).isNotNull();
	}

	private CommunityPostRecord samplePost(UUID postId, UUID publisherId, PostVisibility visibility) {
		return new CommunityPostRecord(
			postId, UUID.randomUUID(), 1L, publisherId,
			visibility, "title", "summary", null, 1,
			visibility == PostVisibility.UNLISTED ? "stored-hash" : null,
			visibility == PostVisibility.UNLISTED ? Instant.now() : null,
			null, ModerationStatus.VISIBLE, Instant.now(), null
		);
	}

	private CommunityPostRecord deletedPost(UUID postId, UUID publisherId, PostVisibility visibility) {
		return new CommunityPostRecord(
			postId, UUID.randomUUID(), 1L, publisherId,
			visibility, "title", "summary", null, 1,
			null, null, null, ModerationStatus.VISIBLE, Instant.now(), Instant.now()
		);
	}

	private CommunityPostDetail sampleDetail(UUID postId) {
		return new CommunityPostDetail(
			postId, UUID.randomUUID(), null, null,
			PostVisibility.PUBLIC, "title", "summary",
			List.of(), 0, 0, 0, 0, false,
			ModerationStatus.VISIBLE, OffsetDateTime.now(), 1,
			new CommunityPostSnapshot(List.of(), List.of(), null),
			List.of(), null, null, null, null
		);
	}
}
