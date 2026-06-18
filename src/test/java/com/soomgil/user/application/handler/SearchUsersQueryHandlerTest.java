package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.application.query.SearchUsersQuery;
import com.soomgil.user.domain.model.UserSummaryRecord;
import com.soomgil.user.infrastructure.persistence.UserSearchMapper;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link SearchUsersQueryHandler} 단위 테스트.
 */
class SearchUsersQueryHandlerTest {

	private final UserSearchMapper mapper = mock(UserSearchMapper.class);
	private final SearchUsersQueryHandler handler = new SearchUsersQueryHandler(mapper);

	@Test
	@DisplayName("검색어를 display_name LIKE 검색하고 결과를 PagedUserSummary로 조립한다")
	void returnsPagedResultForQuery() {
		UUID userId1 = UUID.randomUUID();
		UUID userId2 = UUID.randomUUID();
		when(mapper.search("민", 20, 0)).thenReturn(List.of(
			new UserSummaryRecord(userId1, "민지", null),
			new UserSummaryRecord(userId2, "민준", URI.create("https://cdn.example.com/u2.png"))
		));
		when(mapper.count("민")).thenReturn(2L);

		PagedUserSummary result = handler.handle(new SearchUsersQuery("민", 0, 20));

		assertThat(result.items()).hasSize(2);
		assertThat(result.items().get(0).id()).isEqualTo(userId1);
		assertThat(result.items().get(0).displayName()).isEqualTo("민지");
		assertThat(result.items().get(1).profileImageUrl()).isEqualTo(URI.create("https://cdn.example.com/u2.png"));
		assertThat(result.page().totalElements()).isEqualTo(2L);
		assertThat(result.page().totalPages()).isEqualTo(1);
	}

	@Test
	@DisplayName("빈 검색어는 null로 정규화하여 전체 PUBLIC 프로필을 조회한다")
	void normalizesBlankQueryToNull() {
		when(mapper.search(null, 20, 0)).thenReturn(List.of());
		when(mapper.count(null)).thenReturn(0L);

		PagedUserSummary result = handler.handle(new SearchUsersQuery("   ", 0, 20));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().totalElements()).isEqualTo(0L);
	}

	@Test
	@DisplayName("size가 0 이하면 기본값 20을 사용한다")
	void fallsBackToDefaultSize() {
		when(mapper.search(null, 20, 0)).thenReturn(List.of());
		when(mapper.count(null)).thenReturn(0L);

		handler.handle(new SearchUsersQuery(null, 0, 0));

		org.mockito.Mockito.verify(mapper).search(null, 20, 0);
	}

	@Test
	@DisplayName("size는 최대 100으로 제한한다")
	void capsSizeAtMax() {
		when(mapper.search(null, 100, 0)).thenReturn(List.of());
		when(mapper.count(null)).thenReturn(0L);

		handler.handle(new SearchUsersQuery(null, 0, 500));

		org.mockito.Mockito.verify(mapper).search(null, 100, 0);
	}

	@Test
	@DisplayName("음수 page는 0으로 정규화한다")
	void normalizesNegativePage() {
		when(mapper.search(null, 20, 0)).thenReturn(List.of());
		when(mapper.count(null)).thenReturn(0L);

		handler.handle(new SearchUsersQuery(null, -5, 20));

		org.mockito.Mockito.verify(mapper).search(null, 20, 0);
	}

	@Test
	@DisplayName("전체 개수가 page 경계를 넘으면 totalPages가 올바르게 계산된다")
	void computesTotalPages() {
		when(mapper.search(null, 20, 0)).thenReturn(List.of());
		when(mapper.count(null)).thenReturn(45L);

		PagedUserSummary result = handler.handle(new SearchUsersQuery(null, 0, 20));

		assertThat(result.page().totalPages()).isEqualTo(3);
	}
}
