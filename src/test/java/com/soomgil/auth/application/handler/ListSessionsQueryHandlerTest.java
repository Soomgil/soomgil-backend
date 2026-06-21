package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.api.dto.PagedUserSession;
import com.soomgil.auth.api.dto.UserSession;
import com.soomgil.auth.application.query.ListSessionsQuery;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListSessionsQueryHandlerTest {

	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);

	private final ListSessionsQueryHandler handler = new ListSessionsQueryHandler(userSessionMapper);

	@Test
	@DisplayName("page와 size에 맞춰 세션 목록과 page metadata를 반환한다")
	void returnsPagedSessions() {
		UUID userId = UUID.randomUUID();
		List<UserSession> items = List.of(
			new UserSession(
				UUID.randomUUID(), UUID.randomUUID(), 1, "Chrome", "macOS",
				OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now().plusDays(7),
				null, null
			)
		);
		when(userSessionMapper.findByUserId(eq(userId), anyInt(), anyInt())).thenReturn(items);
		when(userSessionMapper.countByUserId(userId)).thenReturn(15L);

		PagedUserSession result = handler.handle(new ListSessionsQuery(userId, 1, 10));

		assertThat(result.items()).hasSize(1);
		assertThat(result.page().page()).isEqualTo(1);
		assertThat(result.page().size()).isEqualTo(10);
		assertThat(result.page().totalElements()).isEqualTo(15L);
		assertThat(result.page().totalPages()).isEqualTo(2);
		verify(userSessionMapper).findByUserId(userId, 10, 10);
	}

	@Test
	@DisplayName("총 건수가 size의 배수면 totalPages가 정확히 맞아떨어진다")
	void computesTotalPagesExactly() {
		UUID userId = UUID.randomUUID();
		when(userSessionMapper.findByUserId(eq(userId), anyInt(), anyInt())).thenReturn(List.of());
		when(userSessionMapper.countByUserId(userId)).thenReturn(20L);

		PagedUserSession result = handler.handle(new ListSessionsQuery(userId, 0, 10));

		assertThat(result.page().totalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("빈 결과도 page metadata를 포함하여 반환한다")
	void returnsEmptyItemsWithPageMeta() {
		UUID userId = UUID.randomUUID();
		when(userSessionMapper.findByUserId(eq(userId), anyInt(), anyInt())).thenReturn(List.of());
		when(userSessionMapper.countByUserId(userId)).thenReturn(0L);

		PagedUserSession result = handler.handle(new ListSessionsQuery(userId, 0, 10));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().totalElements()).isZero();
		assertThat(result.page().totalPages()).isZero();
	}
}
