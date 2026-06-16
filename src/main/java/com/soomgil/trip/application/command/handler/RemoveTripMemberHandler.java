package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.RemoveTripMemberCommand;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link RemoveTripMemberCommand}를 처리해 active 멤버를 REMOVED 상태로 전환한다.
 *
 * <p>MVP에서는 owner가 다른 member를 제거하는 흐름만 허용한다. owner 제거와 member 자진 탈퇴는
 * 정책 확정 전까지 막는다.
 */
@Component
public class RemoveTripMemberHandler implements CommandHandler<RemoveTripMemberCommand, NoResult> {

	private final TripCommandRepository commandRepository;
	private final TripQueryRepository queryRepository;
	private final TripAccessGuard accessGuard;
	private final TimeProvider timeProvider;

	public RemoveTripMemberHandler(
		TripCommandRepository commandRepository,
		TripQueryRepository queryRepository,
		TimeProvider timeProvider
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
		this.accessGuard = new TripAccessGuard(queryRepository);
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public NoResult handle(RemoveTripMemberCommand command) {
		Objects.requireNonNull(command.tripId(), "tripId must not be null");
		Objects.requireNonNull(command.targetUserId(), "targetUserId must not be null");
		Objects.requireNonNull(command.actorUserId(), "actorUserId must not be null");
		accessGuard.requireOwner(command.tripId(), command.actorUserId());
		validateTarget(command);

		Instant now = timeProvider.now();
		commandRepository.removeTripMember(command.tripId(), command.targetUserId(), command.actorUserId(), now);
		return NoResult.INSTANCE;
	}

	private void validateTarget(RemoveTripMemberCommand command) {
		if (command.targetUserId().equals(command.actorUserId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Trip owner cannot be removed.");
		}
		TripAccessSnapshot targetAccess = queryRepository.findTripAccess(command.tripId(), command.targetUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip member was not found."));
		if (targetAccess.memberStatus() != TripMemberStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip member was not found.");
		}
	}
}
