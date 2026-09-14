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
import com.soomgil.itinerary.application.port.MapMatchingClient;
import com.soomgil.itinerary.application.port.MapMatchClientRequest;
import com.soomgil.itinerary.application.port.MapMatchClientResult;
import com.soomgil.itinerary.application.port.MapMatchingException;
import com.soomgil.itinerary.application.port.RouteCoordinate;
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
 * 저장 경로를 수정하고 협업 이벤트를 기록한다.
 * <p>geometry 없이 mode를 지정하면 기존 경로의 양 끝점 사이를 다시 계산한다.
 * 계산 실패 시 기존 경로와 일정 버전을 유지한다.
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
	private final MapMatchingClient routingClient;

	public UpdateRouteSegmentHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider,
		ObjectMapper objectMapper,
		MapMatchingClient routingClient
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.routingClient = Objects.requireNonNull(routingClient, "routingClient must not be null");
	}

	@Override
	@Transactional
	public ItineraryMutationResult handle(UpdateRouteSegmentCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		RouteSegmentUpdateResult current = repository.findRouteSegment(command.tripId(), command.routeId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Route was not found."));
		if (repository.findItineraryVersion(command.tripId()).orElse(-1L) != command.baseVersion()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match.");
		}

		MapMatchClientResult calculated = null;
		if (command.mode() != null && command.geometry() == null) {
			try {
				calculated = routingClient.match(new MapMatchClientRequest(
					providerProfile(command.mode()), endpoints(current), null, false));
			} catch (MapMatchingException exception) {
				throw new BusinessException(ErrorCode.ROUTE_CALCULATION_FAILED);
			}
		}
		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		RouteSegmentUpdateResult route = repository.updateRouteSegment(new RouteSegmentUpdate(
			command.tripId(),
			command.routeId(),
			command.mode(),
			calculated == null ? null : "MAPBOX",
			command.mode() == null ? null : providerProfile(command.mode()),
			calculated != null ? toJson(calculated.geometry()) : command.geometry() == null ? null : toJson(command.geometry()),
			calculated != null ? calculated.distanceMeters() : command.distanceMeters(),
			calculated != null ? calculated.durationSeconds() : command.durationSeconds(),
			calculated != null ? calculated.confidence() : command.confidence(),
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

	private List<RouteCoordinate> endpoints(RouteSegmentUpdateResult route) {
		try {
			var geometry = objectMapper.readTree(route.geometry());
			var coordinates = geometry.path("coordinates");
			if (!"LineString".equals(geometry.path("type").asText()) || coordinates.size() < 2) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "경로의 출발점과 도착점을 확인할 수 없습니다.");
			}
			var first = coordinates.get(0);
			var last = coordinates.get(coordinates.size() - 1);
			return List.of(coordinate(first), coordinate(last));
		} catch (JsonProcessingException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "저장된 경로 좌표가 올바르지 않습니다.");
		}
	}

	private RouteCoordinate coordinate(com.fasterxml.jackson.databind.JsonNode point) {
		if (!point.isArray() || point.size() < 2 || !point.get(0).isNumber() || !point.get(1).isNumber()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "저장된 경로 좌표가 올바르지 않습니다.");
		}
		double lng = point.get(0).asDouble();
		double lat = point.get(1).asDouble();
		if (!Double.isFinite(lng) || !Double.isFinite(lat) || Math.abs(lng) > 180 || Math.abs(lat) > 90) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "저장된 경로 좌표가 올바르지 않습니다.");
		}
		return new RouteCoordinate(lng, lat);
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
			case CYCLING -> "mapbox/cycling";
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
