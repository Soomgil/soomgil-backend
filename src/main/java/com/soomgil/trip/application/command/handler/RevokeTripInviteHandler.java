package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.application.command.dto.RevokeTripInviteCommand;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link RevokeTripInviteCommand}를 처리해 초대를 취소한다.
 *
 * <p>owner 권한을 확인한 뒤 PENDING 초대를 REVOKED로 전환한다.
 */
@Component
public class RevokeTripInviteHandler implements CommandHandler<RevokeTripInviteCommand, NoResult> {

	private final TripCommandRepository commandRepository;
	private final TripAccessGuard accessGuard;
	private final TimeProvider timeProvider;

	public RevokeTripInviteHandler(
		TripCommandRepository commandRepository,
		TripQueryRepository queryRepository,
		TimeProvider timeProvider
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.accessGuard = new TripAccessGuard(queryRepository);
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public NoResult handle(RevokeTripInviteCommand command) {
		accessGuard.requireOwner(command.tripId(), command.actorUserId());
		commandRepository.revokeTripInvite(command.inviteId(), command.actorUserId(), timeProvider.now());
		return NoResult.INSTANCE;
	}
}
