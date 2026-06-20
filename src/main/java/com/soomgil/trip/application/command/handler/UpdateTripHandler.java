package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.UpdateTripCommand;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripSettingsUpdate;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.trip.domain.model.TripTitlePolicy;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateTripCommand}를 처리해 여행방 기본 설정을 수정한다.
 *
 * <p>멤버 수정 범위는 아직 정책이 열려 있어, 현재는 owner만 수정할 수 있다.
 */
@Component
public class UpdateTripHandler implements CommandHandler<UpdateTripCommand, NoResult> {

	private final TripCommandRepository commandRepository;
	private final TripAccessGuard accessGuard;
	private final TimeProvider timeProvider;

	public UpdateTripHandler(
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
	public NoResult handle(UpdateTripCommand command) {
		Objects.requireNonNull(command.tripId(), "tripId must not be null");
		Objects.requireNonNull(command.actorUserId(), "actorUserId must not be null");
		accessGuard.requireOwner(command.tripId(), command.actorUserId());
		validateStatus(command.status());

		Instant now = timeProvider.now();
		String title = command.title() == null ? null : TripTitlePolicy.normalizeTitle(command.title());
		boolean displayDestinationProvided = command.displayDestination() != null;
		String displayDestination = displayDestinationProvided
			? TripTitlePolicy.normalizeOptionalText(command.displayDestination())
			: null;

		if (title != null || displayDestinationProvided || command.status() != null || command.legalRegionCodesProvided()) {
			commandRepository.updateTrip(new TripSettingsUpdate(
				command.tripId(),
				title,
				displayDestinationProvided,
				displayDestination,
				command.status(),
				now
			));
		}
		if (command.legalRegionCodesProvided()) {
			commandRepository.replaceTripRegions(command.tripId(), normalizedRegionCodes(command.legalRegionCodes()), now);
		}
		return NoResult.INSTANCE;
	}

	private void validateStatus(TripStatus status) {
		if (status == TripStatus.DELETED) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Use delete trip endpoint to delete a trip.");
		}
	}

	private List<String> normalizedRegionCodes(List<String> legalRegionCodes) {
		return new LinkedHashSet<>(legalRegionCodes).stream().toList();
	}
}
