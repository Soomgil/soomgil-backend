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
import com.soomgil.community.application.command.CreateCommunityThreadCommand;
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

class CreateCommunityThreadCommandHandlerTest {

	private final CommunityThreadMapper threadMapper = mock(CommunityThreadMapper.class);
	private final ThreadMediaMapper threadMediaMapper = mock(ThreadMediaMapper.class);
	private final MediaFileQueryService mediaFileQueryService = mock(MediaFileQueryService.class);
	private final CommunityThreadAssembler assembler = mock(CommunityThreadAssembler.class);

	private final CreateCommunityThreadCommandHandler handler = new CreateCommunityThreadCommandHandler(
		threadMapper, threadMediaMapper, mediaFileQueryService, assembler
	);

	@Test
	@DisplayName("이미지 없이 짧은 텍스트만으로 쓰레드를 작성할 수 있다")
	void createsTextOnlyThread() {
		UUID authorId = UUID.randomUUID();
		stubSavedThread(authorId, "성심당 줄이 미쳤어요");

		CommunityThread result = handler.handle(new CreateCommunityThreadCommand(
			authorId, "성심당 줄이 미쳤어요", List.of()
		));

		assertThat(result).isNotNull();
		verify(threadMapper).insert(
			any(UUID.class), eq(authorId), eq("성심당 줄이 미쳤어요"), any(Instant.class)
		);
		verify(threadMediaMapper, never()).insert(any(UUID.class), any(UUID.class), anyInt(), any(Instant.class));
	}

	@Test
	@DisplayName("이미지를 첨부하면 요청 순서대로 sort_order가 부여된다")
	void linksMediaInRequestOrder() {
		UUID authorId = UUID.randomUUID();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		stubSavedThread(authorId, "제주 사진");
		when(mediaFileQueryService.isOwnedActiveMedia(first, authorId)).thenReturn(true);
		when(mediaFileQueryService.isOwnedActiveMedia(second, authorId)).thenReturn(true);

		handler.handle(new CreateCommunityThreadCommand(authorId, "제주 사진", List.of(first, second)));

		verify(threadMediaMapper).insert(any(UUID.class), eq(first), eq(0), any(Instant.class));
		verify(threadMediaMapper).insert(any(UUID.class), eq(second), eq(1), any(Instant.class));
	}

	@Test
	@DisplayName("본문이 비어 있으면 VALIDATION_FAILED로 거부한다")
	void rejectsBlankContent() {
		UUID authorId = UUID.randomUUID();

		assertThatThrownBy(() -> handler.handle(new CreateCommunityThreadCommand(authorId, "   ", List.of())))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));

		verify(threadMapper, never()).insert(any(UUID.class), any(UUID.class), any(String.class), any(Instant.class));
	}

	@Test
	@DisplayName("본문이 500자를 넘으면 VALIDATION_FAILED로 거부한다")
	void rejectsTooLongContent() {
		UUID authorId = UUID.randomUUID();

		assertThatThrownBy(() -> handler.handle(
			new CreateCommunityThreadCommand(authorId, "가".repeat(501), List.of())
		))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("이미지가 5장 이상이면 THREAD_MEDIA_LIMIT_EXCEEDED로 거부한다")
	void rejectsTooManyImages() {
		UUID authorId = UUID.randomUUID();
		List<UUID> five = List.of(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
		);

		assertThatThrownBy(() -> handler.handle(new CreateCommunityThreadCommand(authorId, "사진", five)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_MEDIA_LIMIT_EXCEEDED));

		verify(threadMapper, never()).insert(any(UUID.class), any(UUID.class), any(String.class), any(Instant.class));
	}

	@Test
	@DisplayName("내 소유가 아닌 미디어를 첨부하면 MEDIA_LINK_FORBIDDEN으로 거부한다")
	void rejectsMediaOwnedByAnotherUser() {
		UUID authorId = UUID.randomUUID();
		UUID foreignMedia = UUID.randomUUID();
		when(mediaFileQueryService.isOwnedActiveMedia(foreignMedia, authorId)).thenReturn(false);

		assertThatThrownBy(() -> handler.handle(
			new CreateCommunityThreadCommand(authorId, "사진", List.of(foreignMedia))
		))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.MEDIA_LINK_FORBIDDEN));

		verify(threadMediaMapper, never()).insert(any(UUID.class), any(UUID.class), anyInt(), any(Instant.class));
	}

	private void stubSavedThread(UUID authorId, String content) {
		when(threadMapper.findById(any(UUID.class))).thenAnswer(invocation -> Optional.of(
			new CommunityThreadRecord(
				invocation.getArgument(0), authorId, content,
				ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
			)
		));
		when(assembler.toThread(any(CommunityThreadRecord.class), eq(authorId)))
			.thenReturn(sampleThread(content));
	}

	private CommunityThread sampleThread(String content) {
		return new CommunityThread(
			UUID.randomUUID(), null, content, List.of(), 0, 0, false, true,
			ModerationStatus.VISIBLE, null, OffsetDateTime.now(), OffsetDateTime.now()
		);
	}
}
