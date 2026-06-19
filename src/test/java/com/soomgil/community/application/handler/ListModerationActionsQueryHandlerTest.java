package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PagedModerationAction;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.application.query.ListModerationActionsQuery;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.domain.model.ModerationActionRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ModerationActionMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListModerationActionsQueryHandlerTest {

	private final ModerationActionMapper actionMapper = mock(ModerationActionMapper.class);
	private final CommunityReportAssembler assembler = mock(CommunityReportAssembler.class);

	private final ListModerationActionsQueryHandler handler =
		new ListModerationActionsQueryHandler(actionMapper, assembler);

	@Test
	@DisplayName("조치 이력을 페이지네이션으로 반환한다")
	void listsActionsWithPagination() {
		ModerationActionRecord record = sampleAction();

		when(actionMapper.findAll(0, 20)).thenReturn(List.of(record));
		when(actionMapper.countAll()).thenReturn(1);
		when(assembler.toAction(record)).thenReturn(new ModerationAction(
			record.id(), null, ReportTargetType.POST, UUID.randomUUID(),
			ModerationActionType.HIDE, ModerationStatus.HIDDEN, "spam",
			OffsetDateTime.now()
		));

		PagedModerationAction result = handler.handle(new ListModerationActionsQuery(0, 20));

		assertThat(result.items()).hasSize(1);
		assertThat(result.page().totalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("조치 이력이 없으면 빈 페이지를 반환한다")
	void returnsEmptyPageWhenNoActions() {
		when(actionMapper.findAll(0, 20)).thenReturn(List.of());
		when(actionMapper.countAll()).thenReturn(0);

		PagedModerationAction result = handler.handle(new ListModerationActionsQuery(0, 20));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().totalElements()).isEqualTo(0);
	}

	private ModerationActionRecord sampleAction() {
		return new ModerationActionRecord(
			UUID.randomUUID(), UUID.randomUUID(), ReportTargetType.POST, UUID.randomUUID(),
			ModerationActionType.HIDE, ModerationStatus.HIDDEN, "reason",
			null, Instant.now()
		);
	}
}
