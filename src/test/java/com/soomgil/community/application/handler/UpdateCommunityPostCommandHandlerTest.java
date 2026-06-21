package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.application.command.UpdateCommunityPostCommand;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.HashtagMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostHashtagMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostMediaMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateCommunityPostCommandHandlerTest {

	private final CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
	private final HashtagMapper hashtagMapper = mock(HashtagMapper.class);
	private final PostHashtagMapper postHashtagMapper = mock(PostHashtagMapper.class);
	private final PostMediaMapper postMediaMapper = mock(PostMediaMapper.class);
	private final CommunityPostAssembler assembler = mock(CommunityPostAssembler.class);

	private final UpdateCommunityPostCommandHandler handler = new UpdateCommunityPostCommandHandler(
		postMapper, hashtagMapper, postHashtagMapper, postMediaMapper, assembler
	);

	@Test
	@DisplayName("title만 수정하면 updateBasics가 새 title과 기존 값들로 호출된다")
	void updatesTitleOnly() {
		UUID postId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();
		CommunityPostRecord original = samplePost(postId, publisherId, PostVisibility.PUBLIC);

		when(postMapper.findById(postId)).thenReturn(Optional.of(original), Optional.of(original));
		when(assembler.toDetail(any(), any(), any(), eq(true))).thenReturn(sampleDetail(postId));

		handler.handle(new UpdateCommunityPostCommand(
			postId, publisherId, "새 제목", null, null, null, null, null
		));

		verify(postMapper).updateBasics(
			eq(postId), eq("새 제목"), eq("summary"),
			eq(PostVisibility.PUBLIC), any(), any(Instant.class)
		);
		verify(postMediaMapper, never()).deleteAllByPostId(any());
		verify(postHashtagMapper, never()).deleteAllByPostId(any());
	}

	@Test
	@DisplayName("발행자가 아닌 사용자의 수정 요청은 POST_AUTHOR_REQUIRED를 반환한다")
	void rejectsNonAuthor() {
		UUID postId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, publisherId, PostVisibility.PUBLIC)));

		assertThatThrownBy(() -> handler.handle(new UpdateCommunityPostCommand(
			postId, actorId, "title", null, null, null, null, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.POST_AUTHOR_REQUIRED));
	}

	@Test
	@DisplayName("미디어 목록을 수정하면 기존 미디어를 삭제하고 새로 삽입한다")
	void replacesMedia() {
		UUID postId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();
		CommunityPostRecord post = samplePost(postId, publisherId, PostVisibility.PUBLIC);

		when(postMapper.findById(postId)).thenReturn(Optional.of(post), Optional.of(post));
		when(assembler.toDetail(any(), any(), any(), eq(true))).thenReturn(sampleDetail(postId));

		UUID media1 = UUID.randomUUID();
		UUID media2 = UUID.randomUUID();
		handler.handle(new UpdateCommunityPostCommand(
			postId, publisherId, null, null, null, null, List.of(media1, media2), null
		));

		verify(postMediaMapper).deleteAllByPostId(postId);
		verify(postMediaMapper).insert(any(UUID.class), eq(postId), eq(media1), eq(0), any(), any(Instant.class));
		verify(postMediaMapper).insert(any(UUID.class), eq(postId), eq(media2), eq(1), any(), any(Instant.class));
	}

	@Test
	@DisplayName("삭제된 게시글은 수정할 수 없다")
	void rejectsDeletedPost() {
		UUID postId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(new CommunityPostRecord(
				postId, UUID.randomUUID(), 1L, publisherId,
				PostVisibility.PUBLIC, "title", "summary", null, 1,
				null, null, null, ModerationStatus.VISIBLE, Instant.now(), Instant.now()
			)));

		assertThatThrownBy(() -> handler.handle(new UpdateCommunityPostCommand(
			postId, publisherId, "new title", null, null, null, null, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.POST_NOT_FOUND));
	}

	private CommunityPostRecord samplePost(UUID postId, UUID publisherId, PostVisibility visibility) {
		return new CommunityPostRecord(
			postId, UUID.randomUUID(), 1L, publisherId,
			visibility, "title", "summary", null, 1,
			null, null, null, ModerationStatus.VISIBLE, Instant.now(), null
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
