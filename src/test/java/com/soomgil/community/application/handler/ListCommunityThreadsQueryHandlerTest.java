package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PagedCommunityThread;
import com.soomgil.community.application.query.ListCommunityThreadsQuery;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListCommunityThreadsQueryHandlerTest {

	private final CommunityThreadMapper threadMapper = mock(CommunityThreadMapper.class);
	private final CommunityThreadAssembler assembler = mock(CommunityThreadAssembler.class);

	private final ListCommunityThreadsQueryHandler handler =
		new ListCommunityThreadsQueryHandler(threadMapper, assembler);

	@Test
	@DisplayName("공개 피드는 최신순 page/size 메타데이터와 함께 반환한다")
	void returnsPagedFeed() {
		when(threadMapper.findFeed(eq(null), eq(null), eq(0), eq(20)))
			.thenReturn(List.of(record(), record()));
		when(threadMapper.countFeed(null, null)).thenReturn(42L);
		when(assembler.toThread(any(CommunityThreadRecord.class), any())).thenReturn(thread());

		PagedCommunityThread result = handler.handle(new ListCommunityThreadsQuery(null, null, null, 0, 20));

		assertThat(result.items()).hasSize(2);
		assertThat(result.page().page()).isZero();
		assertThat(result.page().size()).isEqualTo(20);
		assertThat(result.page().totalElements()).isEqualTo(42L);
		assertThat(result.page().totalPages()).isEqualTo(3);
		assertThat(result.page().sort()).containsExactly("createdAt,desc", "id,desc");
	}

	@Test
	@DisplayName("두 번째 페이지는 offset을 page * size로 계산한다")
	void computesOffsetFromPageAndSize() {
		when(threadMapper.findFeed(eq(null), eq(null), eq(40), eq(20))).thenReturn(List.of());
		when(threadMapper.countFeed(null, null)).thenReturn(0L);

		handler.handle(new ListCommunityThreadsQuery(null, null, null, 2, 20));

		verify(threadMapper).findFeed(null, null, 40, 20);
	}

	@Test
	@DisplayName("authorId와 검색어 필터를 mapper에 그대로 전달한다")
	void passesFilters() {
		UUID authorId = UUID.randomUUID();
		when(threadMapper.findFeed(eq(authorId), eq("성심당"), eq(0), eq(10))).thenReturn(List.of());
		when(threadMapper.countFeed(authorId, "성심당")).thenReturn(0L);

		handler.handle(new ListCommunityThreadsQuery(null, authorId, "성심당", 0, 10));

		verify(threadMapper).findFeed(authorId, "성심당", 0, 10);
	}

	@Test
	@DisplayName("size는 1~100 범위로 보정한다")
	void clampsPageSize() {
		when(threadMapper.findFeed(any(), any(), eq(0), eq(100))).thenReturn(List.of());
		when(threadMapper.countFeed(any(), any())).thenReturn(0L);

		handler.handle(new ListCommunityThreadsQuery(null, null, null, 0, 5000));

		verify(threadMapper).findFeed(null, null, 0, 100);
	}

	@Test
	@DisplayName("빈 피드도 정상 page 메타데이터를 반환한다")
	void returnsEmptyPage() {
		when(threadMapper.findFeed(any(), any(), eq(0), eq(20))).thenReturn(List.of());
		when(threadMapper.countFeed(any(), any())).thenReturn(0L);

		PagedCommunityThread result = handler.handle(new ListCommunityThreadsQuery(null, null, null, 0, 20));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().totalElements()).isZero();
		assertThat(result.page().totalPages()).isZero();
	}

	private CommunityThreadRecord record() {
		return new CommunityThreadRecord(
			UUID.randomUUID(), UUID.randomUUID(), "본문", ModerationStatus.VISIBLE,
			null, Instant.now(), Instant.now()
		);
	}

	private CommunityThread thread() {
		return new CommunityThread(
			UUID.randomUUID(), null, "본문", List.of(), 0, 0, false, false,
			ModerationStatus.VISIBLE, null, OffsetDateTime.now(), OffsetDateTime.now()
		);
	}
}
