package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.application.command.dto.CreateTripCommand;
import com.soomgil.trip.application.command.dto.CreateTripResult;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripMember;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateTripCommand}를 처리해 여행방을 생성한다.
 *
 * <p>쓰기 handler이므로 transaction 경계를 가진다. 성공 시 여행방 row와 생성자의
 * ACTIVE MEMBER row가 함께 저장된다.
 */
@Component
public class CreateTripHandler implements CommandHandler<CreateTripCommand, CreateTripResult> {

	private final TripCommandRepository repository;
	private final TimeProvider timeProvider;

	public CreateTripHandler(TripCommandRepository repository, TimeProvider timeProvider) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public CreateTripResult handle(CreateTripCommand command) {
		Objects.requireNonNull(command.creatorUserId(), "creatorUserId must not be null");
		Instant now = timeProvider.now();
		UUID tripId = Ids.newUuid();
		UUID memberId = Ids.newUuid();
		Trip trip = Trip.create(
			tripId,
			command.creatorUserId(),
			command.title(),
			command.displayDestination(),
			now
		);
		TripMember initialMember = TripMember.initialOwnerMember(memberId, tripId, command.creatorUserId(), now);

		repository.saveCreatedTrip(trip, initialMember, command.legalRegionCodes());
		return new CreateTripResult(
			trip.id(),
			trip.ownerUserId(),
			trip.title(),
			trip.displayDestination(),
			trip.status(),
			trip.itineraryVersion(),
			trip.createdAt(),
			initialMember.id()
		);
	}
}
