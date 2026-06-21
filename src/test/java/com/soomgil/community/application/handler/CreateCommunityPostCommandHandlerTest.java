package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.application.command.CreateCommunityPostCommand;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.application.service.CommunityPostSnapshotCodec;
import com.soomgil.community.application.service.ShareTokenService;
import com.soomgil.community.application.service.TripSnapshotChecker;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.domain.model.HashtagRecord;
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

class CreateCommunityPostCommandHandlerTest {

	private final CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
	private final HashtagMapper hashtagMapper = mock(HashtagMapper.class);
	private final PostHashtagMapper postHashtagMapper = mock(PostHashtagMapper.class);
	private final PostMediaMapper postMediaMapper = mock(PostMediaMapper.class);
	private final ShareTokenService shareTokenService = mock(ShareTokenService.class);
	private final TripSnapshotChecker tripSnapshotChecker = mock(TripSnapshotChecker.class);
	private final CommunityPostSnapshotCodec snapshotCodec = mock(CommunityPostSnapshotCodec.class);
	private final CommunityPostAssembler assembler = mock(CommunityPostAssembler.class);

	private final CreateCommunityPostCommandHandler handler = new CreateCommunityPostCommandHandler(
		postMapper, hashtagMapper, postHashtagMapper, postMediaMapper,
		shareTokenService, tripSnapshotChecker, snapshotCodec, assembler
	);

	@Test
	@DisplayName("PUBLIC 게시글 발행 시 공유 토큰 없이 INSERT한다")
	void createsPublicPostWithoutShareToken() {
		UUID publisherId = UUID.randomUUID();
		UUID tripId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();

		CommunityPostSnapshot snapshot = new CommunityPostSnapshot(List.of(), List.of(), null);
		when(tripSnapshotChecker.fetchSnapshot(eq(tripId), eq(1L), eq(publisherId))).thenReturn(snapshot);
		when(snapshotCodec.encode(snapshot)).thenReturn("{\"days\":[]}");
		when(postMapper.findById(any(UUID.class)))
			.thenReturn(Optional.of(samplePost(postId, publisherId, PostVisibility.PUBLIC)));
		when(assembler.toDetail(any(), eq(publisherId), isNull(), eq(true)))
			.thenReturn(sampleDetail(postId));

		CommunityPostDetail result = handler.handle(new CreateCommunityPostCommand(
			tripId, 1L, publisherId, PostVisibility.PUBLIC,
			"제주도 3박 4일", "맛집 코스", null, null, null
		));

		assertThat(result).isNotNull();
		verify(shareTokenService, never()).issue();
		verify(postMapper).insert(
			any(UUID.class), eq(tripId), eq(1L), eq(publisherId),
			eq(PostVisibility.PUBLIC), eq("제주도 3박 4일"), eq("맛집 코스"), isNull(),
			eq(1), eq("{\"days\":[]}"), isNull(), isNull(), isNull(), any(Instant.class)
		);
	}

	@Test
	@DisplayName("UNLISTED 게시글 발행 시 공유 토큰을 발급하고 hash를 저장한다")
	void createsUnlistedPostWithShareToken() {
		UUID publisherId = UUID.randomUUID();
		UUID tripId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();

		when(tripSnapshotChecker.fetchSnapshot(eq(tripId), eq(1L), eq(publisherId)))
			.thenReturn(new CommunityPostSnapshot(List.of(), List.of(), null));
		when(snapshotCodec.encode(any())).thenReturn("{\"days\":[]}");
		when(shareTokenService.issue())
			.thenReturn(new ShareTokenService.IssuedShareToken("raw-token", "hashed-token"));
		when(postMapper.findById(any(UUID.class)))
			.thenReturn(Optional.of(samplePost(postId, publisherId, PostVisibility.UNLISTED)));
		when(assembler.toDetail(any(), eq(publisherId), eq("raw-token"), eq(true)))
			.thenReturn(sampleDetail(postId));

		handler.handle(new CreateCommunityPostCommand(
			tripId, 1L, publisherId, PostVisibility.UNLISTED,
			"비공개 여행", null, null, null, null
		));

		verify(shareTokenService).issue();
		verify(postMapper).insert(
			any(UUID.class), eq(tripId), eq(1L), eq(publisherId),
			eq(PostVisibility.UNLISTED), eq("비공개 여행"), isNull(), isNull(),
			eq(1), eq("{\"days\":[]}"), eq("hashed-token"), any(Instant.class), any(Instant.class), any(Instant.class)
		);
	}

	@Test
	@DisplayName("제목이 비어있으면 VALIDATION_FAILED를 던진다")
	void rejectsBlankTitle() {
		UUID publisherId = UUID.randomUUID();

		assertThatThrownBy(() -> handler.handle(new CreateCommunityPostCommand(
			UUID.randomUUID(), 1L, publisherId, PostVisibility.PUBLIC,
			"", null, null, null, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("해시태그는 정규화하여 중복 제거 후 연결한다")
	void linksHashtagsNormalizedAndDeduped() {
		UUID publisherId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		UUID hashtagId = UUID.randomUUID();

		when(tripSnapshotChecker.fetchSnapshot(any(), eq(1L), any()))
			.thenReturn(new CommunityPostSnapshot(List.of(), List.of(), null));
		when(snapshotCodec.encode(any())).thenReturn("{\"days\":[]}");
		when(hashtagMapper.findByNormalizedName(eq("부산맛집")))
			.thenReturn(Optional.of(new HashtagRecord(hashtagId, "부산맛집", "부산맛집", 0, Instant.now(), Instant.now())));
		when(postMapper.findById(any(UUID.class)))
			.thenReturn(Optional.of(samplePost(postId, publisherId, PostVisibility.PUBLIC)));
		when(assembler.toDetail(any(), any(), isNull(), eq(true)))
			.thenReturn(sampleDetail(postId));

		handler.handle(new CreateCommunityPostCommand(
			UUID.randomUUID(), 1L, publisherId, PostVisibility.PUBLIC,
			"부산 여행", null, null, null,
			List.of("#부산맛집", "부산맛집", "  부산맛집  ")
		));

		verify(hashtagMapper).insertOrIgnore(any(UUID.class), eq("#부산맛집"), eq("부산맛집"), any(Instant.class));
		verify(postHashtagMapper).insert(any(UUID.class), eq(hashtagId), any(Instant.class));
		verify(hashtagMapper).adjustUsageCount(eq(hashtagId), eq(1), any(Instant.class));
	}

	private CommunityPostRecord samplePost(UUID postId, UUID publisherId, PostVisibility visibility) {
		return new CommunityPostRecord(
			postId, UUID.randomUUID(), 1L, publisherId,
			visibility, "title", "summary", null, 1,
			visibility == PostVisibility.UNLISTED ? "hash" : null,
			visibility == PostVisibility.UNLISTED ? Instant.now() : null,
			null, ModerationStatus.VISIBLE, Instant.now(), null
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
