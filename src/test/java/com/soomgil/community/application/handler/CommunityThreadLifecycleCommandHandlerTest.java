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

import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.application.command.DeleteCommunityThreadCommand;
import com.soomgil.community.application.command.UpdateCommunityThreadCommand;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadMediaMapper;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.application.MediaFileQueryService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityThreadLifecycleCommandHandlerTest {

	private final CommunityThreadMapper threadMapper = mock(CommunityThreadMapper.class);
	private final ThreadMediaMapper threadMediaMapper = mock(ThreadMediaMapper.class);
	private final MediaFileQueryService mediaFileQueryService = mock(MediaFileQueryService.class);
	private final CommunityThreadAssembler assembler = mock(CommunityThreadAssembler.class);

	private final UpdateCommunityThreadCommandHandler updateHandler = new UpdateCommunityThreadCommandHandler(
		threadMapper, threadMediaMapper, mediaFileQueryService, assembler
	);
	private final DeleteCommunityThreadCommandHandler deleteHandler =
		new DeleteCommunityThreadCommandHandler(threadMapper);

	@Test
	@DisplayName("작성자는 본문을 수정할 수 있다")
	void authorCanUpdateContent() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));
		when(assembler.toThread(any(CommunityThreadRecord.class), eq(authorId))).thenReturn(sampleThread());

		CommunityThread result = updateHandler.handle(new UpdateCommunityThreadCommand(
			threadId, authorId, "수정한 본문", null
		));

		assertThat(result).isNotNull();
		verify(threadMapper).updateContent(eq(threadId), eq("수정한 본문"), any(Instant.class));
		verify(threadMediaMapper, never()).deleteByThreadId(any(UUID.class));
	}

	@Test
	@DisplayName("mediaFileIds를 전달하면 기존 이미지를 지우고 새 목록으로 교체한다")
	void replacesMediaWhenProvided() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		UUID mediaId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));
		when(assembler.toThread(any(CommunityThreadRecord.class), eq(authorId))).thenReturn(sampleThread());
		when(mediaFileQueryService.isOwnedActiveMedia(mediaId, authorId)).thenReturn(true);

		updateHandler.handle(new UpdateCommunityThreadCommand(threadId, authorId, "사진 교체", List.of(mediaId)));

		verify(threadMediaMapper).deleteByThreadId(threadId);
		verify(threadMediaMapper).insert(eq(threadId), eq(mediaId), eq(0), any(Instant.class));
	}

	@Test
	@DisplayName("작성자가 아니면 수정할 수 없다")
	void nonAuthorCannotUpdate() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));

		assertThatThrownBy(() -> updateHandler.handle(new UpdateCommunityThreadCommand(
			threadId, UUID.randomUUID(), "남의 글 수정", null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_AUTHOR_REQUIRED));

		verify(threadMapper, never()).updateContent(any(UUID.class), any(String.class), any(Instant.class));
	}

	@Test
	@DisplayName("이미 삭제된 쓰레드는 THREAD_NOT_FOUND로 응답한다")
	void deletedThreadIsNotFound() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(new CommunityThreadRecord(
			threadId, authorId, "삭제됨", ModerationStatus.DELETED, Instant.now(), Instant.now(), Instant.now()
		)));

		assertThatThrownBy(() -> updateHandler.handle(new UpdateCommunityThreadCommand(
			threadId, authorId, "수정 시도", null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_NOT_FOUND));
	}

	@Test
	@DisplayName("존재하지 않는 쓰레드 수정은 THREAD_NOT_FOUND로 응답한다")
	void missingThreadIsNotFound() {
		UUID threadId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> updateHandler.handle(new UpdateCommunityThreadCommand(
			threadId, UUID.randomUUID(), "수정 시도", null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_NOT_FOUND));
	}

	@Test
	@DisplayName("수정 본문도 500자 제한을 적용한다")
	void updateRejectsTooLongContent() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));

		assertThatThrownBy(() -> updateHandler.handle(new UpdateCommunityThreadCommand(
			threadId, authorId, "가".repeat(501), null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("수정 시 이미지 5장 이상은 THREAD_MEDIA_LIMIT_EXCEEDED로 거부한다")
	void updateRejectsTooManyImages() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));

		assertThatThrownBy(() -> updateHandler.handle(new UpdateCommunityThreadCommand(
			threadId, authorId, "사진", List.of(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
			)
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_MEDIA_LIMIT_EXCEEDED));

		verify(threadMediaMapper, never()).insert(any(UUID.class), any(UUID.class), anyInt(), any(Instant.class));
	}

	@Test
	@DisplayName("작성자는 쓰레드를 soft delete할 수 있다")
	void authorCanSoftDelete() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));

		deleteHandler.handle(new DeleteCommunityThreadCommand(threadId, authorId));

		verify(threadMapper).softDelete(eq(threadId), eq(authorId), any(Instant.class));
	}

	@Test
	@DisplayName("작성자가 아니면 삭제할 수 없다")
	void nonAuthorCannotDelete() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));

		assertThatThrownBy(() -> deleteHandler.handle(
			new DeleteCommunityThreadCommand(threadId, UUID.randomUUID())
		))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_AUTHOR_REQUIRED));

		verify(threadMapper, never()).softDelete(any(UUID.class), any(UUID.class), any(Instant.class));
	}

	private CommunityThreadRecord visibleThread(UUID threadId, UUID authorId) {
		return new CommunityThreadRecord(
			threadId, authorId, "원본 본문", ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}

	private CommunityThread sampleThread() {
		return new CommunityThread(
			UUID.randomUUID(), null, "본문", List.of(), 0, 0, false, true,
			ModerationStatus.VISIBLE, null, OffsetDateTime.now(), OffsetDateTime.now()
		);
	}
}
