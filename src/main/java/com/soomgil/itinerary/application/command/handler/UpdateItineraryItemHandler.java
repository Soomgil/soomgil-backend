package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemUpdate;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateItineraryItemCommand}를 처리해 itinerary item을 수정한다.
 */
@Component
public class UpdateItineraryItemHandler implements CommandHandler<UpdateItineraryItemCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public UpdateItineraryItemHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public ItineraryMutationResult handle(UpdateItineraryItemCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validateRequest(command);
		ItineraryItemReadModel current = repository.findItem(command.tripId(), command.itemId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item was not found."));
		ItineraryItemUpdate update = toUpdate(command, current);
		if (!update.itineraryDayId().equals(current.itineraryDayId())
			&& !repository.existsDay(command.tripId(), update.itineraryDayId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found.");
		}
		List<UUID> connectedRouteIds = repository.findActiveRouteIdsByItem(command.tripId(), command.itemId());
		if (!connectedRouteIds.isEmpty()
			&& (!update.itineraryDayId().equals(current.itineraryDayId()) || !update.sortOrder().equals(current.sortOrder()))) {
			throw new BusinessException(
				ErrorCode.BUSINESS_RULE_VIOLATION,
				"Route-connected item must not be moved alone."
			);
		}

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		ItineraryItemReadModel updated = repository.updateItem(new ItineraryItemUpdate(
			update.tripId(),
			update.itemId(),
			update.itineraryDayId(),
			update.sortOrder(),
			update.placeName(),
			update.address(),
			update.lat(),
			update.lng(),
			update.thumbnailUrl(),
			command.actorUserId(),
			now
		)).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item was not found."));
		eventRepository.save(ItineraryCollaborationEvents.itemUpdated(
			command.tripId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			current,
			updated,
			now
		));
		return new ItineraryMutationResult(command.tripId(), newVersion, null, toView(updated), null, null, List.of());
	}

	private void validateRequest(UpdateItineraryItemCommand command) {
		if (command.itemId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Item id is required.");
		}
		if (command.itineraryDayId() == null
			&& command.sortOrder() == null
			&& command.placeName() == null
			&& command.address() == null
			&& command.lat() == null
			&& command.lng() == null
			&& command.thumbnailUrl() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "At least one item field is required.");
		}
		if (command.sortOrder() != null && command.sortOrder() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sort order must be greater than or equal to 0.");
		}
		if (command.placeName() != null && command.placeName().isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Place name must not be blank.");
		}
	}

	private ItineraryItemUpdate toUpdate(UpdateItineraryItemCommand command, ItineraryItemReadModel current) {
		URI thumbnailUrl = command.thumbnailUrl() == null ? current.thumbnailUrl() : command.thumbnailUrl();
		return new ItineraryItemUpdate(
			command.tripId(),
			command.itemId(),
			command.itineraryDayId() == null ? current.itineraryDayId() : command.itineraryDayId(),
			command.sortOrder() == null ? current.sortOrder() : command.sortOrder(),
			command.placeName() == null ? current.placeName() : command.placeName().trim(),
			command.address() == null ? current.address() : normalizeText(command.address()),
			command.lat() == null ? current.lat() : command.lat(),
			command.lng() == null ? current.lng() : command.lng(),
			thumbnailUrl == null ? null : thumbnailUrl.toString(),
			command.actorUserId(),
			null
		);
	}

	private ItineraryItemView toView(ItineraryItemReadModel item) {
		return new ItineraryItemView(
			item.id(),
			item.itineraryDayId(),
			item.sortOrder(),
			item.itemType(),
			item.placeProvider(),
			item.externalPlaceId(),
			item.placeName(),
			item.address(),
			item.lat(),
			item.lng(),
			item.thumbnailUrl(),
			item.sourceStatus()
		);
	}

	private String normalizeText(String value) {
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
