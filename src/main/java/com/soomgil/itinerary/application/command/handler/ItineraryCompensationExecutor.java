package com.soomgil.itinerary.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCompensationExecutor;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdate;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
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
		"DELETE_ROUTE_SEGMENT",
		"RESTORE_ITINERARY_DAY",
		"RESTORE_ITINERARY_ITEM",
		"RESTORE_MAP_DRAWING",
		"RESTORE_ROUTE_SEGMENT",
		"UPDATE_ITINERARY_DAY",
		"UPDATE_ITINERARY_ITEM",
		"UPDATE_MAP_DRAWING",
		"UPDATE_ROUTE_SEGMENT"
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
			case "DELETE_ITINERARY_ITEM" -> deleteItem(tripId, actorUserId, command, executedAt);
			case "DELETE_MAP_DRAWING" -> repository.softDeleteMapDrawing(
				tripId, uuid(command, "drawingId"), actorUserId, executedAt);
			case "DELETE_ROUTE_SEGMENT" -> repository.softDeleteRouteSegment(
				tripId, uuid(command, "routeId"), actorUserId, executedAt);
			case "RESTORE_ITINERARY_DAY" -> restoreDay(tripId, command, executedAt);
			case "RESTORE_ITINERARY_ITEM" -> restoreItem(tripId, actorUserId, command, executedAt);
			case "RESTORE_MAP_DRAWING" -> repository.restoreMapDrawing(
				tripId, uuid(command, "drawingId"), actorUserId, executedAt);
			case "RESTORE_ROUTE_SEGMENT" -> repository.restoreRouteSegment(
				tripId, uuid(command, "routeId"), actorUserId, executedAt);
			case "UPDATE_ITINERARY_DAY" -> updateDay(tripId, command, executedAt);
			case "UPDATE_ITINERARY_ITEM" -> updateItem(tripId, actorUserId, command, executedAt);
			case "UPDATE_MAP_DRAWING" -> updateMapDrawing(tripId, actorUserId, command, executedAt);
			case "UPDATE_ROUTE_SEGMENT" -> updateRoute(tripId, actorUserId, command, executedAt);
			default -> false;
		};
		if (!applied) {
			throw new BusinessException(ErrorCode.CONFLICT, "Compensation target has changed or no longer exists.");
		}
	}

	private boolean deleteItem(UUID tripId, UUID actorUserId, JsonNode command, Instant executedAt) {
		if (!repository.softDeleteItem(tripId, uuid(command, "itemId"), actorUserId, executedAt)) {
			return false;
		}
		return routeIds(command).stream()
			.allMatch(routeId -> repository.softDeleteRouteSegment(tripId, routeId, actorUserId, executedAt));
	}

	private boolean restoreItem(UUID tripId, UUID actorUserId, JsonNode command, Instant executedAt) {
		if (!repository.restoreItem(tripId, uuid(command, "itemId"), actorUserId, executedAt)) {
			return false;
		}
		return routeIds(command).stream()
			.allMatch(routeId -> repository.restoreRouteSegment(tripId, routeId, actorUserId, executedAt));
	}

	private List<UUID> routeIds(JsonNode command) {
		List<UUID> routeIds = new ArrayList<>();
		command.path("routeIds").forEach(node -> routeIds.add(UUID.fromString(node.asText())));
		return routeIds;
	}

	private boolean updateMapDrawing(UUID tripId, UUID actorUserId, JsonNode command, Instant executedAt) {
		return repository.updateMapDrawing(new MapDrawingUpdate(
			tripId,
			uuid(command, "drawingId"),
			command.path("geometry").toString(),
			command.path("style").isNull() ? null : command.path("style").toString(),
			nullableText(command, "label"),
			command.path("sortOrder").asInt(),
			null,
			actorUserId,
			executedAt
		)).isPresent();
	}

	private boolean updateRoute(UUID tripId, UUID actorUserId, JsonNode command, Instant executedAt) {
		return repository.updateRouteSegment(new RouteSegmentUpdate(
			tripId,
			uuid(command, "routeId"),
			RouteMode.valueOf(command.path("mode").asText()),
			nullableText(command, "providerProfile"),
			command.path("geometry").toString(),
			nullableDouble(command, "distanceMeters"),
			nullableDouble(command, "durationSeconds"),
			nullableDouble(command, "confidence"),
			actorUserId,
			executedAt
		)).isPresent();
	}

	private boolean updateDay(UUID tripId, JsonNode command, Instant executedAt) {
		return repository.updateDay(new ItineraryDayUpdate(
			tripId,
			uuid(command, "dayId"),
			nullableInt(command, "dayNumber"),
			nullableDate(command, "date"),
			nullableText(command, "title"),
			command.path("sortOrder").asInt(),
			executedAt
		)).isPresent();
	}

	private boolean updateItem(UUID tripId, UUID actorUserId, JsonNode command, Instant executedAt) {
		return repository.updateItem(new ItineraryItemUpdate(
			tripId,
			uuid(command, "itemId"),
			uuid(command, "dayId"),
			command.path("sortOrder").asInt(),
			command.path("placeName").asText(),
			nullableText(command, "address"),
			nullableDouble(command, "lat"),
			nullableDouble(command, "lng"),
			nullableText(command, "thumbnailUrl"),
			actorUserId,
			executedAt
		)).isPresent();
	}

	private boolean restoreDay(UUID tripId, JsonNode command, Instant executedAt) {
		UUID dayId = uuid(command, "dayId");
		if (repository.existsDay(tripId, dayId)) {
			return false;
		}
		repository.insertDay(new ItineraryDayCreate(
			dayId,
			tripId,
			ItineraryDayGroupType.valueOf(command.path("groupType").asText()),
			command.path("dayNumber").isNull() ? null : command.path("dayNumber").asInt(),
			command.path("date").isNull() ? null : LocalDate.parse(command.path("date").asText()),
			command.path("title").isNull() ? null : command.path("title").asText(),
			command.path("sortOrder").asInt(),
			executedAt,
			executedAt
		));
		return true;
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

	private String nullableText(JsonNode command, String field) {
		return command.path(field).isNull() ? null : command.path(field).asText();
	}

	private Integer nullableInt(JsonNode command, String field) {
		return command.path(field).isNull() ? null : command.path(field).asInt();
	}

	private Double nullableDouble(JsonNode command, String field) {
		return command.path(field).isNull() ? null : command.path(field).asDouble();
	}

	private LocalDate nullableDate(JsonNode command, String field) {
		return command.path(field).isNull() ? null : LocalDate.parse(command.path(field).asText());
	}
}
