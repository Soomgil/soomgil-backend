package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.application.command.CreateModerationActionCommand;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.application.service.ModerationAccessGuard;
import com.soomgil.community.application.service.ModerationTargetService;
import com.soomgil.community.domain.model.ModerationActionRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ModerationActionMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모더레이션 조치를 생성하고 대상에 적용한다.
 *
 * <p>모더레이터 권한을 먼저 검증한다. 그 후 {@link ModerationTargetService}로
 * HIDE/RESTORE/DELETE를 대상에 적용하고, 결과를 {@code moderation_actions}에 기록한다.
 */
@Component
@Transactional
public class CreateModerationActionCommandHandler
	implements CommandHandler<CreateModerationActionCommand, ModerationAction> {

	private final ModerationAccessGuard accessGuard;
	private final ModerationTargetService targetService;
	private final ModerationActionMapper actionMapper;
	private final CommunityReportAssembler assembler;

	public CreateModerationActionCommandHandler(
		ModerationAccessGuard accessGuard,
		ModerationTargetService targetService,
		ModerationActionMapper actionMapper,
		CommunityReportAssembler assembler
	) {
		this.accessGuard = accessGuard;
		this.targetService = targetService;
		this.actionMapper = actionMapper;
		this.assembler = assembler;
	}

	@Override
	public ModerationAction handle(CreateModerationActionCommand command) {
		accessGuard.requireModerator(command.moderatorUserId());

		Instant now = Instant.now();
		var resultingStatus = targetService.applyAction(
			command.targetType(), command.targetId(),
			command.action(), command.moderationReason(), now);

		UUID actionId = UUID.randomUUID();
		actionMapper.insert(
			actionId,
			command.moderatorUserId(),
			command.targetType(),
			command.targetId(),
			command.action(),
			resultingStatus,
			command.moderationReason(),
			command.reportId(),
			now
		);

		ModerationActionRecord record = new ModerationActionRecord(
			actionId,
			command.moderatorUserId(),
			command.targetType(),
			command.targetId(),
			command.action(),
			resultingStatus,
			command.moderationReason(),
			command.reportId(),
			now
		);

		return assembler.toAction(record);
	}
}
