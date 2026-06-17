package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.RouteSegmentView;
import com.soomgil.itinerary.application.command.dto.SaveRouteSegmentCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SaveRouteSegmentCommand}를 처리해 확정 route segment를 저장한다.
 *
 * <p>외부 provider 호출은 수행하지 않고, 이미 확정된 snapped route geometry만 저장한다.
 */
@Component
public class SaveRouteSegmentHandler implements CommandHandler<SaveRouteSegmentCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;
	private final ObjectMapper objectMapper;

	public SaveRouteSegmentHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider,
		ObjectMapper objectMapper
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	@Transactional
	public ItineraryMutationResult handle(SaveRouteSegmentCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		if (!repository.existsItem(command.tripId(), command.originItineraryItemId())
			|| !repository.existsItem(command.tripId(), command.destinationItineraryItemId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary item was not found.");
		}

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		RouteSegmentCreate route = new RouteSegmentCreate(
			Ids.newUuid(),
			command.tripId(),
			command.originItineraryItemId(),
			command.destinationItineraryItemId(),
			command.mode(),
			normalizeProvider(command.provider()),
			normalizeProviderProfile(command.providerProfile(), command.mode()),
			GeometryFormat.GEOJSON,
			toJson(command.geometry()),
			command.distanceMeters(),
			command.durationSeconds(),
			command.confidence(),
			command.actorUserId(),
			command.actorUserId(),
			now,
			now
		);
		repository.insertRouteSegment(route);
		eventRepository.save(ItineraryCollaborationEvents.routeSegmentCreated(
			route,
			command.baseVersion(),
			newVersion,
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			null,
			new RouteSegmentView(
				route.id(),
				route.originItineraryItemId(),
				route.destinationItineraryItemId(),
				route.mode(),
				route.provider(),
				route.providerProfile(),
				route.geometryFormat(),
				command.geometry(),
				route.distanceMeters(),
				route.durationSeconds(),
				route.confidence()
			),
			null,
			List.of()
		);
	}

	private void validate(SaveRouteSegmentCommand command) {
		if (command.originItineraryItemId() == null || command.destinationItineraryItemId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route origin and destination are required.");
		}
		if (command.originItineraryItemId().equals(command.destinationItineraryItemId())) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route origin and destination must be different.");
		}
		if (command.mode() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route mode is required.");
		}
		if (command.geometry() == null || command.geometry().isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route geometry is required.");
		}
	}

	private String normalizeProvider(String provider) {
		return provider == null || provider.isBlank() ? "MAPBOX" : provider.trim();
	}

	private String normalizeProviderProfile(String providerProfile, com.soomgil.itinerary.domain.model.RouteMode mode) {
		if (providerProfile != null && !providerProfile.isBlank()) {
			return providerProfile.trim();
		}
		return switch (mode) {
			case DRIVING -> "mapbox/driving";
			case WALKING -> "mapbox/walking";
		};
	}

	private String toJson(Map<String, Object> geometry) {
		try {
			return objectMapper.writeValueAsString(geometry);
		}
		catch (JsonProcessingException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route geometry is invalid.");
		}
	}
}
