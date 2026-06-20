package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.CreateMapDrawingCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.MapDrawingView;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateMapDrawingCommand}를 처리해 저장 지도 도형을 생성한다.
 *
 * <p>preview stroke는 저장하지 않고, 사용자가 저장한 GeoJSON geometry만 영구화한다.
 */
@Component
public class CreateMapDrawingHandler implements CommandHandler<CreateMapDrawingCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;
	private final ObjectMapper objectMapper;

	public CreateMapDrawingHandler(
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
	public ItineraryMutationResult handle(CreateMapDrawingCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		validate(command);
		if (command.itineraryDayId() != null && !repository.existsDay(command.tripId(), command.itineraryDayId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Itinerary day was not found.");
		}

		Instant now = timeProvider.now();
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		MapDrawingCreate drawing = new MapDrawingCreate(
			Ids.newUuid(),
			command.tripId(),
			command.itineraryDayId(),
			command.drawingType(),
			GeometryFormat.GEOJSON,
			toJson(command.geometry(), "Geometry is invalid."),
			command.style() == null ? null : toJson(command.style(), "Style is invalid."),
			normalizeText(command.label()),
			command.sortOrder() == null ? 0 : command.sortOrder(),
			0,
			command.actorUserId(),
			command.actorUserId(),
			now,
			now
		);
		repository.insertMapDrawing(drawing);
		eventRepository.save(ItineraryCollaborationEvents.mapDrawingCreated(
			drawing,
			command.baseVersion(),
			newVersion,
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
				command.geometry(),
				command.style(),
				drawing.label(),
				drawing.sortOrder(),
				drawing.version()
			),
			List.of()
		);
	}

	private void validate(CreateMapDrawingCommand command) {
		if (command.drawingType() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Drawing type is required.");
		}
		if (command.geometry() == null || command.geometry().isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Geometry is required.");
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

	private String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
