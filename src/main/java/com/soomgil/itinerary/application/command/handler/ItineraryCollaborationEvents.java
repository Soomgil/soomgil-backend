package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.itinerary.application.command.dto.ItineraryDayOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemOrderCommand;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
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
			null,
			createdAt
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
			null,
			createdAt
		);
	}

	static CollaborationCommandEvent itineraryReordered(
		UUID tripId,
		UUID actorUserId,
		long versionBefore,
		long versionAfter,
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
			"{\"days\":" + daysJson(days) + "}",
			null,
			null,
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
			null,
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
			null,
			null,
			deletedAt
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
			null,
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
			null,
			null,
			deletedAt
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
}
