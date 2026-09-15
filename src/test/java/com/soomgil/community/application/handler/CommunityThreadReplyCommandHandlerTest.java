package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.application.command.CreateCommunityThreadReplyCommand;
import com.soomgil.community.application.command.DeleteCommunityThreadReplyCommand;
import com.soomgil.community.application.command.UpdateCommunityThreadReplyCommand;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityThreadReplyCommandHandlerTest {

	private final CommunityThreadMapper threadMapper = mock(CommunityThreadMapper.class);
	private final CommunityThreadReplyMapper replyMapper = mock(CommunityThreadReplyMapper.class);
	private final CommunityThreadAssembler assembler = mock(CommunityThreadAssembler.class);

	private final CreateCommunityThreadReplyCommandHandler createHandler =
		new CreateCommunityThreadReplyCommandHandler(threadMapper, replyMapper, assembler);
	private final UpdateCommunityThreadReplyCommandHandler updateHandler =
		new UpdateCommunityThreadReplyCommandHandler(replyMapper, assembler);
	private final DeleteCommunityThreadReplyCommandHandler deleteHandler =
		new DeleteCommunityThreadReplyCommandHandler(threadMapper, replyMapper);

	@Test
	@DisplayName("root 답글은 depth=0으로 저장된다")
	void createsRootReplyWithDepthZero() {
		UUID threadId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		stubSavedReply(threadId, null, 0);

		CommunityThreadReply result = createHandler.handle(new CreateCommunityThreadReplyCommand(
			threadId, userId, null, "저도 갔어요"
		));

		assertThat(result).isNotNull();
		verify(replyMapper).insert(
			any(UUID.class), eq(threadId), eq(null), eq(userId), eq("저도 갔어요"), eq(0), any(Instant.class)
		);
	}

	@Test
	@DisplayName("답글의 답글은 depth=1로 저장된다")
	void createsNestedReplyWithDepthOne() {
		UUID threadId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(replyMapper.findById(parentId)).thenReturn(Optional.of(reply(parentId, threadId, null, 0)));
		stubSavedReply(threadId, parentId, 1);

		createHandler.handle(new CreateCommunityThreadReplyCommand(threadId, userId, parentId, "맞아요"));

		verify(replyMapper).insert(
			any(UUID.class), eq(threadId), eq(parentId), eq(userId), eq("맞아요"), eq(1), any(Instant.class)
		);
	}

	@Test
	@DisplayName("2단계 중첩 답글은 THREAD_REPLY_DEPTH_EXCEEDED로 거부한다")
	void rejectsSecondLevelNesting() {
		UUID threadId = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(replyMapper.findById(parentId)).thenReturn(Optional.of(reply(parentId, threadId, UUID.randomUUID(), 1)));

		assertThatThrownBy(() -> createHandler.handle(new CreateCommunityThreadReplyCommand(
			threadId, UUID.randomUUID(), parentId, "더 깊은 답글"
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_REPLY_DEPTH_EXCEEDED));

		verify(replyMapper, never()).insert(
			any(UUID.class), any(UUID.class), any(), any(UUID.class), any(String.class), anyInt(), any(Instant.class)
		);
	}

	@Test
	@DisplayName("다른 쓰레드의 답글을 부모로 지정하면 거부한다")
	void rejectsParentFromAnotherThread() {
		UUID threadId = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(replyMapper.findById(parentId))
			.thenReturn(Optional.of(reply(parentId, UUID.randomUUID(), null, 0)));

		assertThatThrownBy(() -> createHandler.handle(new CreateCommunityThreadReplyCommand(
			threadId, UUID.randomUUID(), parentId, "잘못된 부모"
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("삭제된 쓰레드에는 답글을 달 수 없다")
	void cannotReplyToDeletedThread() {
		UUID threadId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "삭제됨", ModerationStatus.VISIBLE,
			Instant.now(), Instant.now(), Instant.now()
		)));

		assertThatThrownBy(() -> createHandler.handle(new CreateCommunityThreadReplyCommand(
			threadId, UUID.randomUUID(), null, "답글"
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_NOT_FOUND));
	}

	@Test
	@DisplayName("답글 본문도 500자 제한을 적용한다")
	void rejectsTooLongReply() {
		UUID threadId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));

		assertThatThrownBy(() -> createHandler.handle(new CreateCommunityThreadReplyCommand(
			threadId, UUID.randomUUID(), null, "가".repeat(501)
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("답글 작성자만 답글을 수정할 수 있다")
	void onlyReplyAuthorCanUpdate() {
		UUID threadId = UUID.randomUUID();
		UUID replyId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(replyMapper.findById(replyId)).thenReturn(Optional.of(
			new CommunityThreadReplyRecord(
				replyId, threadId, null, authorId, "원본", 0,
				ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
			)
		));

		assertThatThrownBy(() -> updateHandler.handle(new UpdateCommunityThreadReplyCommand(
			threadId, replyId, UUID.randomUUID(), "남의 답글 수정"
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_AUTHOR_REQUIRED));

		verify(replyMapper, never()).updateContent(any(UUID.class), any(String.class), any(Instant.class));
	}

	@Test
	@DisplayName("쓰레드 작성자는 남의 답글도 삭제할 수 있다")
	void threadAuthorCanDeleteOthersReply() {
		UUID threadId = UUID.randomUUID();
		UUID replyId = UUID.randomUUID();
		UUID threadAuthorId = UUID.randomUUID();
		UUID replyAuthorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(new CommunityThreadRecord(
			threadId, threadAuthorId, "본문", ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		)));
		when(replyMapper.findById(replyId)).thenReturn(Optional.of(
			new CommunityThreadReplyRecord(
				replyId, threadId, null, replyAuthorId, "남의 답글", 0,
				ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
			)
		));

		deleteHandler.handle(new DeleteCommunityThreadReplyCommand(threadId, replyId, threadAuthorId));

		verify(replyMapper).softDelete(eq(replyId), eq(threadAuthorId), any(Instant.class));
	}

	@Test
	@DisplayName("제3자는 답글을 삭제할 수 없다")
	void thirdPartyCannotDeleteReply() {
		UUID threadId = UUID.randomUUID();
		UUID replyId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(replyMapper.findById(replyId)).thenReturn(Optional.of(
			new CommunityThreadReplyRecord(
				replyId, threadId, null, UUID.randomUUID(), "답글", 0,
				ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
			)
		));

		assertThatThrownBy(() -> deleteHandler.handle(new DeleteCommunityThreadReplyCommand(
			threadId, replyId, UUID.randomUUID()
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_AUTHOR_REQUIRED));

		verify(replyMapper, never()).softDelete(any(UUID.class), any(UUID.class), any(Instant.class));
	}

	private void stubSavedReply(UUID threadId, UUID parentId, int depth) {
		when(replyMapper.findById(any(UUID.class))).thenAnswer(invocation -> {
			UUID id = invocation.getArgument(0);
			if (parentId != null && parentId.equals(id)) {
				return Optional.of(reply(parentId, threadId, null, 0));
			}
			return Optional.of(reply(id, threadId, parentId, depth));
		});
		when(assembler.toReply(any(CommunityThreadReplyRecord.class), any()))
			.thenReturn(new CommunityThreadReply(
				UUID.randomUUID(), threadId, parentId, null, "본문", depth, true,
				ModerationStatus.VISIBLE, null, OffsetDateTime.now(), OffsetDateTime.now(), List.of()
			));
	}

	private CommunityThreadReplyRecord reply(UUID id, UUID threadId, UUID parentId, int depth) {
		return new CommunityThreadReplyRecord(
			id, threadId, parentId, UUID.randomUUID(), "본문", depth,
			ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}

	private CommunityThreadRecord visibleThread(UUID threadId) {
		return new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "본문", ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}
}
