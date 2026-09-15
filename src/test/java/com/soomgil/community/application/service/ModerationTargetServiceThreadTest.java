package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityCommentMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 신고/모더레이션 대상이 쓰레드와 답글까지 확장되었는지 검증한다.
 */
class ModerationTargetServiceThreadTest {

	private final CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
	private final CommunityCommentMapper commentMapper = mock(CommunityCommentMapper.class);
	private final CommunityThreadMapper threadMapper = mock(CommunityThreadMapper.class);
	private final CommunityThreadReplyMapper threadReplyMapper = mock(CommunityThreadReplyMapper.class);

	private final ModerationTargetService service = new ModerationTargetService(
		postMapper, commentMapper, threadMapper, threadReplyMapper
	);

	@Test
	@DisplayName("THREAD 신고 대상의 소유자는 쓰레드 작성자다")
	void resolvesThreadOwner() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(visibleThread(threadId, authorId)));

		UUID owner = service.requireTargetAndReturnOwner(ReportTargetType.THREAD, threadId);

		assertThat(owner).isEqualTo(authorId);
	}

	@Test
	@DisplayName("THREAD_REPLY 신고 대상의 소유자는 답글 작성자다")
	void resolvesThreadReplyOwner() {
		UUID replyId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		when(threadReplyMapper.findById(replyId)).thenReturn(Optional.of(visibleReply(replyId, authorId)));

		UUID owner = service.requireTargetAndReturnOwner(ReportTargetType.THREAD_REPLY, replyId);

		assertThat(owner).isEqualTo(authorId);
	}

	@Test
	@DisplayName("삭제된 쓰레드는 신고 대상이 될 수 없다")
	void deletedThreadCannotBeReported() {
		UUID threadId = UUID.randomUUID();
		when(threadMapper.findById(threadId)).thenReturn(Optional.of(new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "삭제됨", ModerationStatus.VISIBLE,
			Instant.now(), Instant.now(), Instant.now()
		)));

		assertThatThrownBy(() -> service.requireTargetAndReturnOwner(ReportTargetType.THREAD, threadId))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND));
	}

	@Test
	@DisplayName("존재하지 않는 답글은 신고 대상이 될 수 없다")
	void missingReplyCannotBeReported() {
		UUID replyId = UUID.randomUUID();
		when(threadReplyMapper.findById(replyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.requireTargetAndReturnOwner(ReportTargetType.THREAD_REPLY, replyId))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND));
	}

	@Test
	@DisplayName("쓰레드 숨김 조치는 moderation_status를 HIDDEN으로 바꾼다")
	void hideThreadUpdatesModerationStatus() {
		UUID threadId = UUID.randomUUID();
		Instant now = Instant.now();

		ModerationStatus result = service.applyAction(
			ReportTargetType.THREAD, threadId, ModerationActionType.HIDE, "스팸", now
		);

		assertThat(result).isEqualTo(ModerationStatus.HIDDEN);
		verify(threadMapper).updateModerationStatus(
			eq(threadId), eq(ModerationStatus.HIDDEN), eq("스팸"), any(), eq(now)
		);
	}

	@Test
	@DisplayName("쓰레드 복구 조치는 moderation_status를 VISIBLE로 되돌린다")
	void restoreThreadUpdatesModerationStatus() {
		UUID threadId = UUID.randomUUID();
		Instant now = Instant.now();

		ModerationStatus result = service.applyAction(
			ReportTargetType.THREAD, threadId, ModerationActionType.RESTORE, "오탐", now
		);

		assertThat(result).isEqualTo(ModerationStatus.VISIBLE);
		verify(threadMapper).updateModerationStatus(
			eq(threadId), eq(ModerationStatus.VISIBLE), eq("오탐"), any(), eq(now)
		);
	}

	@Test
	@DisplayName("쓰레드 삭제 조치는 soft delete를 수행한다")
	void deleteThreadPerformsSoftDelete() {
		UUID threadId = UUID.randomUUID();
		Instant now = Instant.now();

		ModerationStatus result = service.applyAction(
			ReportTargetType.THREAD, threadId, ModerationActionType.DELETE, "권리 침해", now
		);

		assertThat(result).isEqualTo(ModerationStatus.DELETED);
		verify(threadMapper).moderationSoftDelete(threadId, "권리 침해", now);
	}

	@Test
	@DisplayName("답글 삭제 조치도 soft delete를 수행한다")
	void deleteThreadReplyPerformsSoftDelete() {
		UUID replyId = UUID.randomUUID();
		Instant now = Instant.now();

		ModerationStatus result = service.applyAction(
			ReportTargetType.THREAD_REPLY, replyId, ModerationActionType.DELETE, "괴롭힘", now
		);

		assertThat(result).isEqualTo(ModerationStatus.DELETED);
		verify(threadReplyMapper).moderationSoftDelete(replyId, "괴롭힘", now);
	}

	private CommunityThreadRecord visibleThread(UUID threadId, UUID authorId) {
		return new CommunityThreadRecord(
			threadId, authorId, "본문", ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}

	private CommunityThreadReplyRecord visibleReply(UUID replyId, UUID authorId) {
		return new CommunityThreadReplyRecord(
			replyId, UUID.randomUUID(), null, authorId, "답글", 0,
			ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}
}
