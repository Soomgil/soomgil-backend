package com.soomgil.community.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.application.command.CreateModerationActionCommand;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.application.service.ModerationAccessGuard;
import com.soomgil.community.application.service.ModerationTargetService;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.infrastructure.persistence.mapper.ModerationActionMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateModerationActionCommandHandlerTest {

	private final ModerationAccessGuard accessGuard = mock(ModerationAccessGuard.class);
	private final ModerationTargetService targetService = mock(ModerationTargetService.class);
	private final ModerationActionMapper actionMapper = mock(ModerationActionMapper.class);
	private final CommunityReportAssembler assembler = mock(CommunityReportAssembler.class);

	private final CreateModerationActionCommandHandler handler =
		new CreateModerationActionCommandHandler(accessGuard, targetService, actionMapper, assembler);

	@Test
	@DisplayName("POST HIDE - 대상의 moderation_status를 HIDDEN으로 변경한다")
	void hidesPost() {
		UUID moderatorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		doNothing().when(accessGuard).requireModerator(moderatorId);
		when(targetService.applyAction(
			eq(ReportTargetType.POST), eq(targetId), eq(ModerationActionType.HIDE), any(), any()))
			.thenReturn(ModerationStatus.HIDDEN);
		when(assembler.toAction(any())).thenReturn(new ModerationAction(
			UUID.randomUUID(), null, ReportTargetType.POST, targetId,
			ModerationActionType.HIDE, ModerationStatus.HIDDEN, "spam",
			OffsetDateTime.now()
		));

		ModerationAction result = handler.handle(new CreateModerationActionCommand(
			moderatorId, ReportTargetType.POST, targetId,
			ModerationActionType.HIDE, "spam", null
		));

		assertThat(result.action()).isEqualTo(ModerationActionType.HIDE);
		assertThat(result.moderationStatus()).isEqualTo(ModerationStatus.HIDDEN);
		verify(actionMapper).insert(
			any(UUID.class), eq(moderatorId), eq(ReportTargetType.POST), eq(targetId),
			eq(ModerationActionType.HIDE), eq(ModerationStatus.HIDDEN),
			eq("spam"), eq(null), any()
		);
	}

	@Test
	@DisplayName("POST DELETE - 대상을 soft delete한다")
	void deletesPost() {
		UUID moderatorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		doNothing().when(accessGuard).requireModerator(moderatorId);
		when(targetService.applyAction(
			eq(ReportTargetType.POST), eq(targetId), eq(ModerationActionType.DELETE), any(), any()))
			.thenReturn(ModerationStatus.DELETED);
		when(assembler.toAction(any())).thenReturn(new ModerationAction(
			UUID.randomUUID(), null, ReportTargetType.POST, targetId,
			ModerationActionType.DELETE, ModerationStatus.DELETED, "violated",
			OffsetDateTime.now()
		));

		handler.handle(new CreateModerationActionCommand(
			moderatorId, ReportTargetType.POST, targetId,
			ModerationActionType.DELETE, "violated", null
		));

		verify(targetService).applyAction(
			eq(ReportTargetType.POST), eq(targetId), eq(ModerationActionType.DELETE),
			eq("violated"), any()
		);
	}

	@Test
	@DisplayName("POST_COMMENT RESTORE - 댓글 moderation_status를 VISIBLE로 변경한다")
	void restoresComment() {
		UUID moderatorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		doNothing().when(accessGuard).requireModerator(moderatorId);
		when(targetService.applyAction(
			eq(ReportTargetType.POST_COMMENT), eq(targetId), eq(ModerationActionType.RESTORE), any(), any()))
			.thenReturn(ModerationStatus.VISIBLE);
		when(assembler.toAction(any())).thenReturn(new ModerationAction(
			UUID.randomUUID(), null, ReportTargetType.POST_COMMENT, targetId,
			ModerationActionType.RESTORE, ModerationStatus.VISIBLE, "restored",
			OffsetDateTime.now()
		));

		handler.handle(new CreateModerationActionCommand(
			moderatorId, ReportTargetType.POST_COMMENT, targetId,
			ModerationActionType.RESTORE, "restored", null
		));

		verify(targetService).applyAction(
			eq(ReportTargetType.POST_COMMENT), eq(targetId), eq(ModerationActionType.RESTORE),
			eq("restored"), any()
		);
	}

	@Test
	@DisplayName("비모더레이터 접근 시 MODERATION_ACCESS_DENIED를 반환한다")
	void rejectsNonModerator() {
		UUID moderatorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		doThrow(new CommunityException(ErrorCode.MODERATION_ACCESS_DENIED))
			.when(accessGuard).requireModerator(moderatorId);

		assertThatThrownBy(() -> handler.handle(new CreateModerationActionCommand(
			moderatorId, ReportTargetType.POST, targetId,
			ModerationActionType.HIDE, null, null
		)))
			.isInstanceOf(CommunityException.class)
			.satisfies(ex -> assertThat(((CommunityException) ex).errorCode())
				.isEqualTo(ErrorCode.MODERATION_ACCESS_DENIED));
	}
}
