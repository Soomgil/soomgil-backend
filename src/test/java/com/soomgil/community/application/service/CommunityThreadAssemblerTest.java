package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadLikeMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadMediaMapper;
import com.soomgil.media.application.MediaFileQueryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityThreadAssemblerTest {

	private final FindDisplayNameQueryHandler displayNameQueryHandler = mock(FindDisplayNameQueryHandler.class);
	private final ThreadMediaMapper threadMediaMapper = mock(ThreadMediaMapper.class);
	private final ThreadLikeMapper threadLikeMapper = mock(ThreadLikeMapper.class);
	private final CommunityThreadReplyMapper replyMapper = mock(CommunityThreadReplyMapper.class);
	private final MediaFileQueryService mediaFileQueryService = mock(MediaFileQueryService.class);

	private final CommunityThreadAssembler assembler = new CommunityThreadAssembler(
		displayNameQueryHandler, threadMediaMapper, threadLikeMapper, replyMapper, mediaFileQueryService
	);

	@BeforeEach
	void stubAuthorLookup() {
		when(displayNameQueryHandler.handle(any(FindDisplayNameQuery.class))).thenReturn("소윤");
		when(displayNameQueryHandler.findProfileImageUrl(any(FindDisplayNameQuery.class))).thenReturn(null);
		when(threadMediaMapper.findMediaFileIdsByThreadId(any(UUID.class))).thenReturn(List.of());
		when(mediaFileQueryService.findByIds(any())).thenReturn(List.of());
	}

	@Test
	@DisplayName("작성자가 조회하면 editableByMe=true다")
	void marksThreadEditableForAuthor() {
		UUID authorId = UUID.randomUUID();
		UUID threadId = UUID.randomUUID();
		when(threadLikeMapper.countByThreadId(threadId)).thenReturn(5);
		when(threadLikeMapper.existsByThreadIdAndUserId(threadId, authorId)).thenReturn(true);
		when(replyMapper.countVisibleByThreadId(threadId)).thenReturn(2);

		CommunityThread result = assembler.toThread(visibleThread(threadId, authorId), authorId);

		assertThat(result.editableByMe()).isTrue();
		assertThat(result.likedByMe()).isTrue();
		assertThat(result.likeCount()).isEqualTo(5);
		assertThat(result.replyCount()).isEqualTo(2);
		assertThat(result.content()).isEqualTo("성심당 줄이 미쳤어요");
	}

	@Test
	@DisplayName("비로그인 조회자는 likedByMe와 editableByMe가 모두 false다")
	void anonymousViewerCannotEditOrLike() {
		UUID threadId = UUID.randomUUID();

		CommunityThread result = assembler.toThread(visibleThread(threadId, UUID.randomUUID()), null);

		assertThat(result.editableByMe()).isFalse();
		assertThat(result.likedByMe()).isFalse();
	}

	@Test
	@DisplayName("삭제된 쓰레드는 본문을 노출하지 않고 tombstone으로 응답한다")
	void deletedThreadIsTombstoned() {
		UUID threadId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		CommunityThreadRecord deleted = new CommunityThreadRecord(
			threadId, authorId, "비밀 본문", ModerationStatus.VISIBLE,
			Instant.now(), Instant.now(), Instant.now()
		);

		CommunityThread result = assembler.toThread(deleted, authorId);

		assertThat(result.content()).isNull();
		assertThat(result.deletedAt()).isNotNull();
		assertThat(result.editableByMe()).isFalse();
	}

	@Test
	@DisplayName("숨김 처리된 쓰레드도 본문을 노출하지 않는다")
	void hiddenThreadIsTombstoned() {
		UUID threadId = UUID.randomUUID();
		CommunityThreadRecord hidden = new CommunityThreadRecord(
			threadId, UUID.randomUUID(), "숨긴 본문", ModerationStatus.HIDDEN,
			null, Instant.now(), Instant.now()
		);

		CommunityThread result = assembler.toThread(hidden, null);

		assertThat(result.content()).isNull();
		assertThat(result.moderationStatus()).isEqualTo(ModerationStatus.HIDDEN);
	}

	@Test
	@DisplayName("삭제된 답글도 tombstone으로 응답한다")
	void deletedReplyIsTombstoned() {
		UUID replyId = UUID.randomUUID();
		CommunityThreadReplyRecord deleted = new CommunityThreadReplyRecord(
			replyId, UUID.randomUUID(), null, UUID.randomUUID(), "삭제된 답글", 0,
			ModerationStatus.VISIBLE, Instant.now(), Instant.now(), Instant.now()
		);

		CommunityThreadReply result = assembler.toReply(deleted, null);

		assertThat(result.content()).isNull();
		assertThat(result.deletedAt()).isNotNull();
	}

	@Test
	@DisplayName("답글 트리는 root 답글 아래에 1단계 답글만 중첩한다")
	void buildsOneLevelReplyTree() {
		UUID threadId = UUID.randomUUID();
		UUID rootA = UUID.randomUUID();
		UUID rootB = UUID.randomUUID();
		CommunityThreadReplyRecord a = reply(rootA, threadId, null, 0);
		CommunityThreadReplyRecord b = reply(rootB, threadId, null, 0);
		CommunityThreadReplyRecord childOfA = reply(UUID.randomUUID(), threadId, rootA, 1);

		List<CommunityThreadReply> tree = assembler.toReplyTree(List.of(a, b), List.of(childOfA), null);

		assertThat(tree).hasSize(2);
		assertThat(tree.get(0).id()).isEqualTo(rootA);
		assertThat(tree.get(0).replies()).hasSize(1);
		assertThat(tree.get(0).replies().get(0).depth()).isEqualTo(1);
		assertThat(tree.get(1).id()).isEqualTo(rootB);
		assertThat(tree.get(1).replies()).isEmpty();
	}

	private CommunityThreadRecord visibleThread(UUID threadId, UUID authorId) {
		return new CommunityThreadRecord(
			threadId, authorId, "성심당 줄이 미쳤어요", ModerationStatus.VISIBLE,
			null, Instant.now(), Instant.now()
		);
	}

	private CommunityThreadReplyRecord reply(UUID id, UUID threadId, UUID parentId, int depth) {
		return new CommunityThreadReplyRecord(
			id, threadId, parentId, UUID.randomUUID(), "답글", depth,
			ModerationStatus.VISIBLE, null, Instant.now(), Instant.now()
		);
	}
}
