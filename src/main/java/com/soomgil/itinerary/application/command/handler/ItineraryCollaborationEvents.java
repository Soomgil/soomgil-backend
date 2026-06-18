package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.itinerary.application.command.dto.ItineraryDayOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemOrderCommand;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdateResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * itinerary write 결과를 collaboration command event로 변환한다.
 */
final class ItineraryCollaborationEvents {

	private static final String SOURCE_USER = "USER";
	private static final String AGGREGATE_DAY = "ITINERARY_DAY";
	private static final String AGGREGATE_ITEM = "ITINERARY_ITEM";
	private static final String AGGREGATE_ITINERARY = "ITINERARY";
	private static final String AGGREGATE_DRAWING = "MAP_DRAWING";
	private static final String AGGREGATE_ROUTE = "ROUTE_SEGMENT";

	private ItineraryCollaborationEvents() {
	}

	static CollaborationCommandEvent dayCreated(
		ItineraryDayCreate day,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		Instant createdAt
	) {
		return new CollaborationCommandEvent(
			day.tripId(),
			actorUserId,
			null,
			SOURCE_USER,
			"CREATE_ITINERARY_DAY",
			AGGREGATE_DAY,
			day.id(),
			versionBefore,
			versionAfter,
			"{\"dayId\":\"" + day.id() + "\",\"groupType\":\"" + day.groupType().name() + "\",\"sortOrder\":" + day.sortOrder() + "}",
			"{\"action\":\"DELETE_ITINERARY_DAY\",\"dayId\":\"" + day.id() + "\"}",
			dayRestorePayload(day),
			createdAt
		);
	}

	static CollaborationCommandEvent dayUpdated(
		UUID tripId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		ItineraryDayReadModel before,
		ItineraryDayReadModel after,
		Instant updatedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"UPDATE_ITINERARY_DAY",
			AGGREGATE_DAY,
			after.id(),
			versionBefore,
			versionAfter,
			dayUpdatePayload(after),
			dayUpdatePayload(before),
			dayUpdatePayload(after),
			updatedAt
		);
	}

	static CollaborationCommandEvent dayDeleted(
		UUID tripId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		ItineraryDayReadModel day,
		Instant deletedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"DELETE_ITINERARY_DAY",
			AGGREGATE_DAY,
			day.id(),
			versionBefore,
			versionAfter,
			"{\"dayId\":\"" + day.id() + "\"}",
			dayRestorePayload(day),
			"{\"action\":\"DELETE_ITINERARY_DAY\",\"dayId\":\"" + day.id() + "\"}",
			deletedAt
		);
	}

	static CollaborationCommandEvent itemCreated(
		ItineraryItemCreate item,
		long versionBefore,
		long versionAfter,
		Instant createdAt
	) {
		return new CollaborationCommandEvent(
			item.tripId(),
			item.createdByUserId(),
			null,
			SOURCE_USER,
			"CREATE_ITINERARY_ITEM",
			AGGREGATE_ITEM,
			item.id(),
			versionBefore,
			versionAfter,
			"{\"itemId\":\"" + item.id() + "\",\"dayId\":\"" + item.itineraryDayId() + "\",\"sortOrder\":" + item.sortOrder() + "}",
			"{\"action\":\"DELETE_ITINERARY_ITEM\",\"itemId\":\"" + item.id() + "\"}",
			"{\"action\":\"RESTORE_ITINERARY_ITEM\",\"itemId\":\"" + item.id() + "\"}",
			createdAt
		);
	}

	static CollaborationCommandEvent itemDeleted(
		UUID tripId,
		UUID itemId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		List<UUID> affectedRouteIds,
		Instant deletedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"DELETE_ITINERARY_ITEM",
			AGGREGATE_ITEM,
			itemId,
			versionBefore,
			versionAfter,
			"{\"itemId\":\"" + itemId + "\",\"affectedRouteIds\":" + uuidArrayJson(affectedRouteIds) + "}",
			itemDeletePayload("RESTORE_ITINERARY_ITEM", itemId, affectedRouteIds),
			itemDeletePayload("DELETE_ITINERARY_ITEM", itemId, affectedRouteIds),
			deletedAt
		);
	}

	static CollaborationCommandEvent itemUpdated(
		UUID tripId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		ItineraryItemReadModel before,
		ItineraryItemReadModel after,
		Instant updatedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"UPDATE_ITINERARY_ITEM",
			AGGREGATE_ITEM,
			after.id(),
			versionBefore,
			versionAfter,
			itemUpdatePayload(after),
			itemUpdatePayload(before),
			itemUpdatePayload(after),
			updatedAt
		);
	}

	static CollaborationCommandEvent itineraryReordered(
		UUID tripId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		List<ItineraryDayOrderCommand> before,
		List<ItineraryDayOrderCommand> days,
		Instant createdAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"REORDER_ITINERARY",
			AGGREGATE_ITINERARY,
			tripId,
			versionBefore,
			versionAfter,
			reorderPayload(days),
			reorderPayload(before),
			reorderPayload(days),
			createdAt
		);
	}

	static CollaborationCommandEvent mapDrawingCreated(
		MapDrawingCreate drawing,
		long versionBefore,
		long versionAfter,
		Instant createdAt
	) {
		return new CollaborationCommandEvent(
			drawing.tripId(),
			drawing.createdByUserId(),
			null,
			SOURCE_USER,
			"CREATE_MAP_DRAWING",
			AGGREGATE_DRAWING,
			drawing.id(),
			versionBefore,
			versionAfter,
			"{\"drawingId\":\"" + drawing.id() + "\",\"drawingType\":\"" + drawing.drawingType().name() + "\",\"sortOrder\":" + drawing.sortOrder() + "}",
			"{\"action\":\"DELETE_MAP_DRAWING\",\"drawingId\":\"" + drawing.id() + "\"}",
			"{\"action\":\"RESTORE_MAP_DRAWING\",\"drawingId\":\"" + drawing.id() + "\"}",
			createdAt
		);
	}

	static CollaborationCommandEvent mapDrawingDeleted(
		UUID tripId,
		UUID drawingId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		Instant deletedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"DELETE_MAP_DRAWING",
			AGGREGATE_DRAWING,
			drawingId,
			versionBefore,
			versionAfter,
			"{\"drawingId\":\"" + drawingId + "\"}",
			"{\"action\":\"RESTORE_MAP_DRAWING\",\"drawingId\":\"" + drawingId + "\"}",
			"{\"action\":\"DELETE_MAP_DRAWING\",\"drawingId\":\"" + drawingId + "\"}",
			deletedAt
		);
	}

	static CollaborationCommandEvent mapDrawingUpdated(
		UUID tripId,
		UUID drawingId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		MapDrawingUpdateResult before,
		MapDrawingUpdateResult after,
		Instant updatedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"UPDATE_MAP_DRAWING",
			AGGREGATE_DRAWING,
			drawingId,
			versionBefore,
			versionAfter,
			mapDrawingUpdatePayload(after),
			mapDrawingUpdatePayload(before),
			mapDrawingUpdatePayload(after),
			updatedAt
		);
	}

	static CollaborationCommandEvent routeSegmentCreated(
		RouteSegmentCreate route,
		long versionBefore,
		long versionAfter,
		Instant createdAt
	) {
		return new CollaborationCommandEvent(
			route.tripId(),
			route.createdByUserId(),
			null,
			SOURCE_USER,
			"CREATE_ROUTE_SEGMENT",
			AGGREGATE_ROUTE,
			route.id(),
			versionBefore,
			versionAfter,
			"{\"routeId\":\"" + route.id() + "\",\"originItemId\":\"" + route.originItineraryItemId()
				+ "\",\"destinationItemId\":\"" + route.destinationItineraryItemId() + "\",\"mode\":\"" + route.mode().name() + "\"}",
			"{\"action\":\"DELETE_ROUTE_SEGMENT\",\"routeId\":\"" + route.id() + "\"}",
			"{\"action\":\"RESTORE_ROUTE_SEGMENT\",\"routeId\":\"" + route.id() + "\"}",
			createdAt
		);
	}

	static CollaborationCommandEvent routeSegmentDeleted(
		UUID tripId,
		UUID routeId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		Instant deletedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"DELETE_ROUTE_SEGMENT",
			AGGREGATE_ROUTE,
			routeId,
			versionBefore,
			versionAfter,
			"{\"routeId\":\"" + routeId + "\"}",
			"{\"action\":\"RESTORE_ROUTE_SEGMENT\",\"routeId\":\"" + routeId + "\"}",
			"{\"action\":\"DELETE_ROUTE_SEGMENT\",\"routeId\":\"" + routeId + "\"}",
			deletedAt
		);
	}

	static CollaborationCommandEvent routeSegmentUpdated(
		UUID tripId,
		UUID routeId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
		RouteSegmentUpdateResult before,
		RouteSegmentUpdateResult after,
		Instant updatedAt
	) {
		return new CollaborationCommandEvent(
			tripId,
			actorUserId,
			null,
			SOURCE_USER,
			"UPDATE_ROUTE_SEGMENT",
			AGGREGATE_ROUTE,
			routeId,
			versionBefore,
			versionAfter,
			routeUpdatePayload(after),
			routeUpdatePayload(before),
			routeUpdatePayload(after),
			updatedAt
		);
	}

	private static String daysJson(List<ItineraryDayOrderCommand> days) {
		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < days.size(); index++) {
			if (index > 0) {
				builder.append(',');
			}
			ItineraryDayOrderCommand day = days.get(index);
			builder.append("{\"dayId\":\"")
				.append(day.dayId())
				.append("\",\"sortOrder\":")
				.append(day.sortOrder())
				.append(",\"items\":")
				.append(itemsJson(day.itemOrders()))
				.append('}');
		}
		return builder.append(']').toString();
	}

	private static String reorderPayload(List<ItineraryDayOrderCommand> days) {
		return "{\"action\":\"REORDER_ITINERARY\",\"days\":" + daysJson(days) + "}";
	}

	private static String dayRestorePayload(ItineraryDayCreate day) {
		return "{\"action\":\"RESTORE_ITINERARY_DAY\",\"dayId\":\"" + day.id()
			+ "\",\"groupType\":\"" + day.groupType().name()
			+ "\",\"dayNumber\":" + nullable(day.dayNumber())
			+ ",\"date\":" + quoted(day.date() == null ? null : day.date().toString())
			+ ",\"title\":" + quoted(day.title())
			+ ",\"sortOrder\":" + day.sortOrder() + "}";
	}

	private static String dayRestorePayload(ItineraryDayReadModel day) {
		return "{\"action\":\"RESTORE_ITINERARY_DAY\",\"dayId\":\"" + day.id()
			+ "\",\"groupType\":\"" + day.groupType().name()
			+ "\",\"dayNumber\":" + nullable(day.dayNumber())
			+ ",\"date\":" + quoted(day.date() == null ? null : day.date().toString())
			+ ",\"title\":" + quoted(day.title())
			+ ",\"sortOrder\":" + day.sortOrder() + "}";
	}

	private static String itemDeletePayload(String action, UUID itemId, List<UUID> routeIds) {
		return "{\"action\":\"" + action + "\",\"itemId\":\"" + itemId
			+ "\",\"routeIds\":" + uuidArrayJson(routeIds) + "}";
	}

	private static String dayUpdatePayload(ItineraryDayReadModel day) {
		return "{\"action\":\"UPDATE_ITINERARY_DAY\",\"dayId\":\"" + day.id()
			+ "\",\"dayNumber\":" + nullable(day.dayNumber())
			+ ",\"date\":" + quoted(day.date() == null ? null : day.date().toString())
			+ ",\"title\":" + quoted(day.title())
			+ ",\"sortOrder\":" + day.sortOrder() + "}";
	}

	private static String itemUpdatePayload(ItineraryItemReadModel item) {
		return "{\"action\":\"UPDATE_ITINERARY_ITEM\",\"itemId\":\"" + item.id()
			+ "\",\"dayId\":\"" + item.itineraryDayId()
			+ "\",\"sortOrder\":" + item.sortOrder()
			+ ",\"placeName\":" + quoted(item.placeName())
			+ ",\"address\":" + quoted(item.address())
			+ ",\"lat\":" + nullable(item.lat())
			+ ",\"lng\":" + nullable(item.lng())
			+ ",\"thumbnailUrl\":" + quoted(item.thumbnailUrl() == null ? null : item.thumbnailUrl().toString()) + "}";
	}

	private static String mapDrawingUpdatePayload(MapDrawingUpdateResult drawing) {
		return "{\"action\":\"UPDATE_MAP_DRAWING\",\"drawingId\":\"" + drawing.id()
			+ "\",\"geometry\":" + drawing.geometry()
			+ ",\"style\":" + (drawing.style() == null ? "null" : drawing.style())
			+ ",\"label\":" + quoted(drawing.label())
			+ ",\"sortOrder\":" + drawing.sortOrder() + "}";
	}

	private static String routeUpdatePayload(RouteSegmentUpdateResult route) {
		return "{\"action\":\"UPDATE_ROUTE_SEGMENT\",\"routeId\":\"" + route.id()
			+ "\",\"mode\":\"" + route.mode().name()
			+ "\",\"providerProfile\":" + quoted(route.providerProfile())
			+ ",\"geometry\":" + route.geometry()
			+ ",\"distanceMeters\":" + nullable(route.distanceMeters())
			+ ",\"durationSeconds\":" + nullable(route.durationSeconds())
			+ ",\"confidence\":" + nullable(route.confidence()) + "}";
	}

	private static String nullable(Object value) {
		return value == null ? "null" : value.toString();
	}

	private static String quoted(String value) {
		if (value == null) {
			return "null";
		}
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static String itemsJson(List<ItineraryItemOrderCommand> items) {
		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < items.size(); index++) {
			if (index > 0) {
				builder.append(',');
			}
			ItineraryItemOrderCommand item = items.get(index);
			builder.append("{\"itemId\":\"")
				.append(item.itemId())
				.append("\",\"sortOrder\":")
				.append(item.sortOrder())
				.append('}');
		}
		return builder.append(']').toString();
	}

	private static String uuidArrayJson(List<UUID> ids) {
		StringBuilder builder = new StringBuilder("[");
		for (int index = 0; index < ids.size(); index++) {
			if (index > 0) {
				builder.append(',');
			}
			builder.append('"').append(ids.get(index)).append('"');
		}
		return builder.append(']').toString();
	}
}
