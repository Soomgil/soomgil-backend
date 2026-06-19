package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.application.command.dto.DeleteTripCommand;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteTripCommand}를 처리해 여행방을 soft delete 상태로 전환한다.
 */
@Component
public class DeleteTripHandler implements CommandHandler<DeleteTripCommand, NoResult> {

	private final TripCommandRepository commandRepository;
	private final TripAccessGuard accessGuard;
	private final TimeProvider timeProvider;

	public DeleteTripHandler(
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
	public NoResult handle(DeleteTripCommand command) {
		Objects.requireNonNull(command.tripId(), "tripId must not be null");
		Objects.requireNonNull(command.actorUserId(), "actorUserId must not be null");
		accessGuard.requireOwner(command.tripId(), command.actorUserId());
		Instant now = timeProvider.now();
		commandRepository.softDeleteTrip(command.tripId(), now);
		return NoResult.INSTANCE;
	}
}
