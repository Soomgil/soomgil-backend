package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.MapDrawingView;
import com.soomgil.itinerary.application.command.dto.UpdateMapDrawingCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.domain.policy.MapDrawingObjectPolicy;
import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link UpdateMapDrawingCommand}를 처리해 저장 지도 도형을 수정한다.
 */
@Component
public class UpdateMapDrawingHandler implements CommandHandler<UpdateMapDrawingCommand, ItineraryMutationResult> {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;
	private final ObjectMapper objectMapper;
	private final MapDrawingObjectPolicy objectPolicy;
	private final MapObjectLeaseStore leaseStore;

	@Autowired
	public UpdateMapDrawingHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider,
		ObjectMapper objectMapper,
		MapDrawingObjectPolicy objectPolicy,
		MapObjectLeaseStore leaseStore
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.objectPolicy = Objects.requireNonNull(objectPolicy, "objectPolicy must not be null");
		this.leaseStore = Objects.requireNonNull(leaseStore, "leaseStore must not be null");
	}

	public UpdateMapDrawingHandler(
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
		this.objectPolicy = new MapDrawingObjectPolicy();
		this.leaseStore = null;
	}

	@Override
	@Transactional
	public ItineraryMutationResult handle(UpdateMapDrawingCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		MapDrawingUpdateResult current = repository.findMapDrawing(command.tripId(), command.drawingId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found."));

		Instant now = timeProvider.now();
		if (leaseStore != null) {
			leaseStore.requireOwned(
				command.tripId(), command.drawingId(), command.actorUserId(), command.websocketSessionId(), now
			);
		}
		Map<String, Object> effectiveTransform = command.transform() == null
			? (current.transform() == null ? null : toMap(current.transform()))
			: command.transform();
		objectPolicy.validate(current.drawingType(), current.mediaFileId(), current.stickerCode(), effectiveTransform);
		Map<String, Object> geometryUpdate = command.geometry();
		if (isMapObject(current.drawingType()) && command.transform() != null && geometryUpdate == null) {
			geometryUpdate = pointGeometry(command.transform());
		}
		Map<String, Object> effectiveGeometry = geometryUpdate == null ? toMap(current.geometry()) : geometryUpdate;
		if (isMapObject(current.drawingType())) {
			validatePointMatchesTransform(effectiveGeometry, effectiveTransform);
		}
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		MapDrawingUpdateResult drawing = repository.updateMapDrawing(new MapDrawingUpdate(
			command.tripId(),
			command.drawingId(),
			geometryUpdate == null ? null : toJson(geometryUpdate, "Geometry is invalid."),
			command.style() == null ? null : toJson(command.style(), "Style is invalid."),
			normalizeText(command.label()),
			command.transform() == null ? null : toJson(command.transform(), "Transform is invalid."),
			command.sortOrder(),
			command.drawingVersion(),
			command.actorUserId(),
			now
		)).orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Map drawing version does not match."));
		eventRepository.save(ItineraryCollaborationEvents.mapDrawingUpdated(
			command.tripId(),
			command.drawingId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			current,
			drawing,
			command.websocketSessionId(),
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			null,
			null,
			new MapDrawingView(
				drawing.id(),
				drawing.itineraryDayId(),
				drawing.drawingType(),
				drawing.geometryFormat(),
				toMap(drawing.geometry()),
				drawing.style() == null ? null : toMap(drawing.style()),
				drawing.label(),
				drawing.mediaFileId(),
				drawing.stickerCode(),
				drawing.transform() == null ? null : toMap(drawing.transform()),
				drawing.sortOrder(),
				drawing.version()
			),
			List.of()
		);
	}

	private boolean isMapObject(DrawingType type) {
		return type == DrawingType.STICKER || type == DrawingType.IMAGE;
	}

	private Map<String, Object> pointGeometry(Map<String, Object> transform) {
		return Map.of(
			"type", "Point",
			"coordinates", List.of(transform.get("centerLng"), transform.get("centerLat"))
		);
	}

	private void validatePointMatchesTransform(Map<String, Object> geometry, Map<String, Object> transform) {
		Object coordinatesValue = geometry.get("coordinates");
		if (!"Point".equals(geometry.get("type"))
			|| !(coordinatesValue instanceof List<?> coordinates) || coordinates.size() != 2
			|| !(coordinates.get(0) instanceof Number longitude)
			|| !(coordinates.get(1) instanceof Number latitude)
			|| Math.abs(longitude.doubleValue() - ((Number) transform.get("centerLng")).doubleValue()) > 0.0000001
			|| Math.abs(latitude.doubleValue() - ((Number) transform.get("centerLat")).doubleValue()) > 0.0000001) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Point geometry must match the map object center.");
		}
	}

	private void validate(UpdateMapDrawingCommand command) {
		if (command.drawingId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Drawing id is required.");
		}
		if (command.geometry() == null
			&& command.style() == null
			&& command.label() == null
			&& command.transform() == null
			&& command.sortOrder() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "At least one drawing field is required.");
		}
		if (command.geometry() != null && command.geometry().isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Geometry must not be empty.");
		}
		if (command.sortOrder() != null && command.sortOrder() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Sort order must be greater than or equal to 0.");
		}
	}

	private String toJson(Map<String, Object> value, String errorMessage) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, errorMessage);
		}
	}

	private Map<String, Object> toMap(String json) {
		try {
			return objectMapper.readValue(json, MAP_TYPE);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Map drawing JSON column is invalid.", exception);
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
