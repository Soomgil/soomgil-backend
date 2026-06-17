package com.soomgil.itinerary.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateItineraryItemCommand}를 처리해 일정 item을 생성한다.
 *
 * <p>item은 같은 여행방의 기존 day에만 추가되며, 성공하면 trip itinerary version을 증가시킨다.
 */
@Component
public class CreateItineraryItemHandler implements CommandHandler<CreateItineraryItemCommand, ItineraryMutationResult> {

	private static final String SOURCE_STATUS_AVAILABLE = "AVAILABLE";

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public CreateItineraryItemHandler(
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
	public ItineraryMutationResult handle(CreateItineraryItemCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		if (!repository.existsDay(command.tripId(), command.itineraryDayId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found.");
		}

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		ItineraryItemCreate item = new ItineraryItemCreate(
			Ids.newUuid(),
			command.tripId(),
			command.itineraryDayId(),
			command.sortOrder(),
			command.itemType(),
			normalizeText(command.placeProvider()),
			normalizeText(command.externalPlaceId()),
			command.placeName().trim(),
			normalizeText(command.address()),
			command.lat(),
			command.lng(),
			command.thumbnailUrl() == null ? null : command.thumbnailUrl().toString(),
			SOURCE_STATUS_AVAILABLE,
			command.actorUserId(),
			command.actorUserId(),
			now,
			now
		);
		repository.insertItem(item);
		eventRepository.save(ItineraryCollaborationEvents.itemCreated(
			item,
			command.baseVersion(),
			newVersion,
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			new ItineraryItemView(
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
				item.thumbnailUrl() == null ? null : java.net.URI.create(item.thumbnailUrl()),
				item.sourceStatus()
			),
			null,
			List.of()
		);
	}

	private void validate(CreateItineraryItemCommand command) {
		if (command.itemType() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Item type is required.");
		}
		if (command.sortOrder() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sort order must be greater than or equal to 0.");
		}
		if (command.placeName() == null || command.placeName().isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Place name is required.");
		}
		if (command.itemType() == ItineraryItemType.PLACE
			&& (command.placeProvider() == null || command.placeProvider().isBlank()
				|| command.externalPlaceId() == null || command.externalPlaceId().isBlank())) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Place reference is required for PLACE item.");
		}
	}

	private String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
