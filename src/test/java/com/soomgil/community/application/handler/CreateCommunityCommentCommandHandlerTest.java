package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.CommunityComment;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
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
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateCommunityCommentCommandHandlerTest {

	private final CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
	private final CommunityCommentMapper commentMapper = mock(CommunityCommentMapper.class);
	private final CommunityPostAssembler assembler = mock(CommunityPostAssembler.class);

	private final CreateCommunityCommentCommandHandler handler =
		new CreateCommunityCommentCommandHandler(postMapper, commentMapper, assembler);

	@Test
	@DisplayName("최상위 댓글을 작성하면 depth=0으로 INSERT된다")
	void createsTopLevelComment() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, userId)));
		when(commentMapper.findById(any(UUID.class)))
			.thenReturn(Optional.of(sampleComment(UUID.randomUUID(), postId, userId, null, 0)));
		when(assembler.toComment(any()))
			.thenReturn(new CommunityComment(
				UUID.randomUUID(), postId, null, null, "좋아요!",
				0, ModerationStatus.VISIBLE, null, OffsetDateTime.now()
			));

		CommunityComment result = handler.handle(new CreateCommunityCommentCommand(
			postId, userId, null, "좋아요!"
		));

		assertThat(result).isNotNull();
		verify(commentMapper).insert(
			any(UUID.class), eq(postId), eq(null), eq(userId), eq("좋아요!"), eq(0), any(Instant.class)
		);
	}

	@Test
	@DisplayName("대댓글은 부모 댓글의 depth+1로 작성된다")
	void createsReplyWithCorrectDepth() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, userId)));
		when(commentMapper.findById(any(UUID.class)))
			.thenReturn(Optional.of(sampleComment(UUID.randomUUID(), postId, userId, parentId, 1)));
		when(commentMapper.findById(eq(parentId)))
			.thenReturn(Optional.of(sampleComment(parentId, postId, userId, null, 0)));
		when(assembler.toComment(any()))
			.thenReturn(new CommunityComment(
				UUID.randomUUID(), postId, parentId, null, "대댓글",
				1, ModerationStatus.VISIBLE, null, OffsetDateTime.now()
			));

		handler.handle(new CreateCommunityCommentCommand(
			postId, userId, parentId, "대댓글"
		));

		verify(commentMapper).insert(
			any(UUID.class), eq(postId), eq(parentId), eq(userId), eq("대댓글"), eq(1), any(Instant.class)
		);
	}

	@Test
	@DisplayName("빈 내용의 댓글은 VALIDATION_FAILED를 반환한다")
	void rejectsBlankContent() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, userId)));

		assertThatThrownBy(() -> handler.handle(new CreateCommunityCommentCommand(
			postId, userId, null, ""
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("최대 깊이를 초과하는 대댓글은 VALIDATION_FAILED를 반환한다")
	void rejectsExceedingMaxDepth() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();

		when(postMapper.findById(postId))
			.thenReturn(Optional.of(samplePost(postId, userId)));
		when(commentMapper.findById(parentId))
			.thenReturn(Optional.of(sampleComment(parentId, postId, userId, null,
				CommunityPostPolicy.COMMENT_MAX_DEPTH)));

		assertThatThrownBy(() -> handler.handle(new CreateCommunityCommentCommand(
			postId, userId, parentId, "too deep"
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	private CommunityPostRecord samplePost(UUID postId, UUID userId) {
		return new CommunityPostRecord(
			postId, UUID.randomUUID(), 1L, userId,
			PostVisibility.PUBLIC, "title", "summary", null, 1,
			null, null, null, ModerationStatus.VISIBLE, Instant.now(), null
		);
	}

	private CommunityCommentRecord sampleComment(
		UUID id, UUID postId, UUID userId, UUID parentId, int depth
	) {
		return new CommunityCommentRecord(
			id, postId, parentId, userId, "content", depth,
			ModerationStatus.VISIBLE, null, Instant.now()
		);
	}
}
