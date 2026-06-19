package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.api.dto.PagedSecurityEvent;
import com.soomgil.auth.application.query.ListSecurityEventsQuery;
import com.soomgil.auth.infrastructure.persistence.SecurityEventMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListSecurityEventsQueryHandlerTest {

	private final SecurityEventMapper securityEventMapper = mock(SecurityEventMapper.class);

	private final ListSecurityEventsQueryHandler handler = new ListSecurityEventsQueryHandler(securityEventMapper);

	@Test
	@DisplayName("page와 size에 맞춰 보안 이벤트 목록과 page metadata를 반환한다")
	void returnsPagedSecurityEvents() {
		UUID userId = UUID.randomUUID();
		List<SecurityEventMapper.SecurityEventRow> rows = List.of(
			new SecurityEventMapper.SecurityEventRow(1L, "LOGIN", true, null, Instant.now())
		);
		when(securityEventMapper.findByUserId(eq(userId), anyInt(), anyInt())).thenReturn(rows);
		when(securityEventMapper.countByUserId(userId)).thenReturn(25L);

		PagedSecurityEvent result = handler.handle(new ListSecurityEventsQuery(userId, 2, 10));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).id()).isEqualTo(1L);
		assertThat(result.items().get(0).eventType()).isEqualTo("LOGIN");
		assertThat(result.page().page()).isEqualTo(2);
		assertThat(result.page().size()).isEqualTo(10);
		assertThat(result.page().totalElements()).isEqualTo(25L);
		assertThat(result.page().totalPages()).isEqualTo(3);
		verify(securityEventMapper).findByUserId(userId, 20, 10);
	}

	@Test
	@DisplayName("빈 결과도 page metadata를 포함하여 반환한다")
	void returnsEmptyItemsWithPageMeta() {
		UUID userId = UUID.randomUUID();
		when(securityEventMapper.findByUserId(eq(userId), anyInt(), anyInt())).thenReturn(List.of());
		when(securityEventMapper.countByUserId(userId)).thenReturn(0L);

		PagedSecurityEvent result = handler.handle(new ListSecurityEventsQuery(userId, 0, 10));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().totalElements()).isZero();
		assertThat(result.page().totalPages()).isZero();
	}

	@Test
	@DisplayName("failureReason이 포함된 이벤트도 그대로 DTO로 변환한다")
	void mapsFailureReasonToDto() {
		UUID userId = UUID.randomUUID();
		List<SecurityEventMapper.SecurityEventRow> rows = List.of(
			new SecurityEventMapper.SecurityEventRow(7L, "LOGIN", false, "INVALID_CREDENTIALS", Instant.now())
		);
		when(securityEventMapper.findByUserId(eq(userId), anyInt(), anyInt())).thenReturn(rows);
		when(securityEventMapper.countByUserId(userId)).thenReturn(1L);

		PagedSecurityEvent result = handler.handle(new ListSecurityEventsQuery(userId, 0, 10));

		assertThat(result.items().get(0).success()).isFalse();
		assertThat(result.items().get(0).failureReason()).isEqualTo("INVALID_CREDENTIALS");
	}
}
