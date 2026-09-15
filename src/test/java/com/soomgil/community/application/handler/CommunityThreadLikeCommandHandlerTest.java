package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.application.command.LikeCommunityThreadCommand;
import com.soomgil.community.application.command.UnlikeCommunityThreadCommand;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadLikeMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityThreadLikeCommandHandlerTest {

	private final CommunityThreadMapper threadMapper = mock(CommunityThreadMapper.class);
	private final ThreadLikeMapper threadLikeMapper = mock(ThreadLikeMapper.class);

	private final LikeCommunityThreadCommandHandler likeHandler =
		new LikeCommunityThreadCommandHandler(threadMapper, threadLikeMapper);
	private final UnlikeCommunityThreadCommandHandler unlikeHandler =
		new UnlikeCommunityThreadCommandHandler(threadMapper, threadLikeMapper);

	@Test
	@DisplayName("좋아요를 누르면 liked=true와 현재 좋아요 수를 반환한다")
	void likeReturnsSummary() {
		UUID threadId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(threadLikeMapper.countByThreadId(threadId)).thenReturn(3);

		CommunityThreadReactionSummary result = likeHandler.handle(
			new LikeCommunityThreadCommand(threadId, userId)
		);

		assertThat(result.liked()).isTrue();
		assertThat(result.likeCount()).isEqualTo(3);
		verify(threadLikeMapper).insertIfAbsent(eq(threadId), eq(userId), any(Instant.class));
	}

	@Test
	@DisplayName("같은 사용자가 좋아요를 두 번 눌러도 멱등하게 처리된다")
	void likeIsIdempotent() {
		UUID threadId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(threadLikeMapper.countByThreadId(threadId)).thenReturn(1);

		CommunityThreadReactionSummary first = likeHandler.handle(new LikeCommunityThreadCommand(threadId, userId));
		CommunityThreadReactionSummary second = likeHandler.handle(new LikeCommunityThreadCommand(threadId, userId));

		assertThat(first.likeCount()).isEqualTo(1);
		assertThat(second.likeCount()).isEqualTo(1);
		assertThat(second.liked()).isTrue();
		verify(threadLikeMapper, times(2)).insertIfAbsent(eq(threadId), eq(userId), any(Instant.class));
	}

	@Test
	@DisplayName("좋아요 취소도 멱등하며 liked=false를 반환한다")
	void unlikeIsIdempotent() {
		UUID threadId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId)));
		when(threadLikeMapper.countByThreadId(threadId)).thenReturn(0);

		CommunityThreadReactionSummary result = unlikeHandler.handle(
			new UnlikeCommunityThreadCommand(threadId, userId)
		);

		assertThat(result.liked()).isFalse();
		assertThat(result.likeCount()).isZero();
		verify(threadLikeMapper).delete(threadId, userId);
	}

	@Test
	@DisplayName("삭제된 쓰레드에는 좋아요를 누를 수 없다")
	void cannotLikeDeletedThread() {
		UUID threadId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "삭제됨", ModerationStatus.VISIBLE,
			Instant.now(), Instant.now(), Instant.now()
		)));

		assertThatThrownBy(() -> likeHandler.handle(new LikeCommunityThreadCommand(threadId, UUID.randomUUID())))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_NOT_FOUND));
	}

	@Test
	@DisplayName("숨김 처리된 쓰레드에는 좋아요를 누를 수 없다")
	void cannotLikeHiddenThread() {
		UUID threadId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "숨김", ModerationStatus.HIDDEN, null, Instant.now(), Instant.now()
		)));

		assertThatThrownBy(() -> likeHandler.handle(new LikeCommunityThreadCommand(threadId, UUID.randomUUID())))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.THREAD_NOT_FOUND));
	}

	private CommunityThreadRecord visibleThread(UUID threadId) {
		return new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "본문", ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}
}
