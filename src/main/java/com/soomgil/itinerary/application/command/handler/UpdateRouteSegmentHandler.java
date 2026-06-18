package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.RouteSegmentView;
import com.soomgil.itinerary.application.command.dto.UpdateRouteSegmentCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.RouteSegmentUpdate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdateResult;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateRouteSegmentCommand}를 처리해 저장 route segment를 수정한다.
 */
@Component
public class UpdateRouteSegmentHandler implements CommandHandler<UpdateRouteSegmentCommand, ItineraryMutationResult> {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;
	private final ObjectMapper objectMapper;

	public UpdateRouteSegmentHandler(
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
	public ItineraryMutationResult handle(UpdateRouteSegmentCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		RouteSegmentUpdateResult current = repository.findRouteSegment(command.tripId(), command.routeId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Route was not found."));

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		RouteSegmentUpdateResult route = repository.updateRouteSegment(new RouteSegmentUpdate(
			command.tripId(),
			command.routeId(),
			command.mode(),
			command.mode() == null ? null : providerProfile(command.mode()),
			command.geometry() == null ? null : toJson(command.geometry()),
			command.distanceMeters(),
			command.durationSeconds(),
			command.confidence(),
			command.actorUserId(),
			now
		)).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Route was not found."));
		eventRepository.save(ItineraryCollaborationEvents.routeSegmentUpdated(
			command.tripId(),
			command.routeId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			current,
			route,
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			null,
			toView(route),
			null,
			List.of(command.routeId())
		);
	}

	private void validate(UpdateRouteSegmentCommand command) {
		if (command.routeId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route id is required.");
		}
		if (command.mode() == null
			&& command.geometry() == null
			&& command.distanceMeters() == null
			&& command.durationSeconds() == null
			&& command.confidence() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "At least one route field is required.");
		}
		if (command.geometry() != null && command.geometry().isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Route geometry must not be empty.");
		}
		if (command.distanceMeters() != null && command.distanceMeters() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Distance must be greater than or equal to 0.");
		}
		if (command.durationSeconds() != null && command.durationSeconds() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Duration must be greater than or equal to 0.");
		}
		if (command.confidence() != null && (command.confidence() < 0 || command.confidence() > 1)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Confidence must be between 0 and 1.");
		}
	}

	private RouteSegmentView toView(RouteSegmentUpdateResult route) {
		return new RouteSegmentView(
			route.id(),
			route.originItineraryItemId(),
			route.destinationItineraryItemId(),
			route.mode(),
			route.provider(),
			route.providerProfile(),
			route.geometryFormat(),
			toMap(route.geometry()),
			route.distanceMeters(),
			route.durationSeconds(),
			route.confidence()
		);
	}

	private String providerProfile(RouteMode mode) {
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

	private Map<String, Object> toMap(String json) {
		try {
			return objectMapper.readValue(json, MAP_TYPE);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Route geometry JSON column is invalid.", exception);
		}
	}
}
