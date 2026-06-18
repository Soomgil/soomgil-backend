package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCompensationExecutor;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * itinerary 생성 command의 삭제 보상을 실행한다.
 */
@Component
public class ItineraryCompensationExecutor implements CollaborationCompensationExecutor {

	private static final Set<String> SUPPORTED_ACTIONS = Set.of(
		"DELETE_ITINERARY_DAY",
		"DELETE_ITINERARY_ITEM",
		"DELETE_MAP_DRAWING",
		"DELETE_ROUTE_SEGMENT"
	);

	private final ItineraryCommandRepository repository;
	private final ObjectMapper objectMapper;

	public ItineraryCompensationExecutor(ItineraryCommandRepository repository, ObjectMapper objectMapper) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean supports(String commandPayload) {
		try {
			return commandPayload != null && SUPPORTED_ACTIONS.contains(read(commandPayload).path("action").asText());
		}
		catch (BusinessException exception) {
			return false;
		}
	}

	@Override
	public void execute(UUID tripId, UUID actorUserId, String commandPayload, Instant executedAt) {
		JsonNode command = read(commandPayload);
		String action = command.path("action").asText();
		boolean applied = switch (action) {
			case "DELETE_ITINERARY_DAY" -> repository.deleteDay(tripId, uuid(command, "dayId"));
			case "DELETE_ITINERARY_ITEM" -> repository.softDeleteItem(
				tripId, uuid(command, "itemId"), actorUserId, executedAt);
			case "DELETE_MAP_DRAWING" -> repository.softDeleteMapDrawing(
				tripId, uuid(command, "drawingId"), actorUserId, executedAt);
			case "DELETE_ROUTE_SEGMENT" -> repository.softDeleteRouteSegment(
				tripId, uuid(command, "routeId"), actorUserId, executedAt);
			default -> false;
		};
		if (!applied) {
			throw new BusinessException(ErrorCode.CONFLICT, "Compensation target has changed or no longer exists.");
		}
	}

	private JsonNode read(String payload) {
		try {
			return objectMapper.readTree(payload);
		}
		catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Compensation command payload is invalid.");
		}
	}

	private UUID uuid(JsonNode command, String field) {
		try {
			return UUID.fromString(command.path(field).asText());
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Compensation target ID is invalid.");
		}
	}
}
