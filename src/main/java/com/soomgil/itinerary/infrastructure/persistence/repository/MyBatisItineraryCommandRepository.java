package com.soomgil.itinerary.infrastructure.persistence.repository;

import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryDayUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.MapDrawingSnapshotUpdate;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdateResult;
import com.soomgil.itinerary.application.port.RouteSegmentSnapshotUpdate;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.itinerary.infrastructure.persistence.mapper.ItineraryCommandMapper;
import com.soomgil.itinerary.infrastructure.persistence.row.ItineraryItemRow;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 일정 day/item 쓰기 repository.
 */
@Repository
public class MyBatisItineraryCommandRepository implements ItineraryCommandRepository {

	private final ItineraryCommandMapper mapper;

	public MyBatisItineraryCommandRepository(ItineraryCommandMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt) {
		Long version = mapper.incrementItineraryVersion(tripId, baseVersion, updatedAt);
		return version == null ? OptionalLong.empty() : OptionalLong.of(version);
	}

	@Override
	public OptionalLong findItineraryVersion(UUID tripId) {
		Long version = mapper.findItineraryVersion(tripId);
		return version == null ? OptionalLong.empty() : OptionalLong.of(version);
	}

	@Override
	public void insertDay(ItineraryDayCreate day) {
		mapper.insertDay(day);
	}

	@Override
	public Optional<ItineraryDayReadModel> findDay(UUID tripId, UUID dayId) {
		return Optional.ofNullable(mapper.findDay(tripId, dayId));
	}

	@Override
	public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
		return Optional.ofNullable(mapper.findUnscheduledDay(tripId));
	}

	@Override
	public Optional<ItineraryDayReadModel> updateDay(ItineraryDayUpdate update) {
		return Optional.ofNullable(mapper.updateDay(update));
	}

	@Override
	public long countActiveItemsByDay(UUID tripId, UUID dayId) {
		return mapper.countActiveItemsByDay(tripId, dayId);
	}

	@Override
	public boolean deleteDay(UUID tripId, UUID dayId) {
		return mapper.deleteDay(tripId, dayId) > 0;
	}

	@Override
	public void insertItem(ItineraryItemCreate item) {
		mapper.insertItem(item);
	}

	@Override
	public Optional<ItineraryItemReadModel> findItem(UUID tripId, UUID itemId) {
		return Optional.ofNullable(mapper.findItem(tripId, itemId))
			.map(this::toItemReadModel);
	}

	@Override
	public Optional<ItineraryItemReadModel> updateItem(ItineraryItemUpdate update) {
		return Optional.ofNullable(mapper.updateItem(update))
			.map(this::toItemReadModel);
	}

	@Override
	public void insertMapDrawing(MapDrawingCreate drawing) {
		mapper.insertMapDrawing(drawing);
	}

	@Override
	public void insertRouteSegment(RouteSegmentCreate route) {
		mapper.insertRouteSegment(route);
	}

	@Override
	public Optional<RouteSegmentUpdateResult> updateRouteSegment(RouteSegmentUpdate update) {
		return Optional.ofNullable(mapper.updateRouteSegment(update));
	}

	@Override
	public Optional<RouteSegmentUpdateResult> applyRouteSegmentSnapshot(RouteSegmentSnapshotUpdate update) {
		return Optional.ofNullable(mapper.applyRouteSegmentSnapshot(update));
	}

	@Override
	public Optional<RouteSegmentUpdateResult> findRouteSegment(UUID tripId, UUID routeId) {
		return Optional.ofNullable(mapper.findRouteSegment(tripId, routeId));
	}

	@Override
	public Long insertRouteMatchRequest(RouteMatchRequestLog request) {
		return mapper.insertRouteMatchRequest(request);
	}

	@Override
	public boolean existsActiveRouteSegment(UUID tripId, UUID routeId) {
		return mapper.existsActiveRouteSegment(tripId, routeId);
	}

	@Override
	public boolean softDeleteRouteSegment(UUID tripId, UUID routeId, UUID deletedByUserId, Instant deletedAt) {
		return mapper.softDeleteRouteSegment(tripId, routeId, deletedByUserId, deletedAt) > 0;
	}

	@Override
	public boolean existsActiveMapDrawing(UUID tripId, UUID drawingId) {
		return mapper.existsActiveMapDrawing(tripId, drawingId);
	}

	@Override
	public boolean softDeleteMapDrawing(UUID tripId, UUID drawingId, UUID deletedByUserId, Instant deletedAt) {
		return mapper.softDeleteMapDrawing(tripId, drawingId, deletedByUserId, deletedAt) > 0;
	}

	@Override
	public Optional<MapDrawingUpdateResult> updateMapDrawing(MapDrawingUpdate update) {
		return Optional.ofNullable(mapper.updateMapDrawing(update));
	}

	@Override
	public Optional<MapDrawingUpdateResult> applyMapDrawingSnapshot(MapDrawingSnapshotUpdate update) {
		return Optional.ofNullable(mapper.applyMapDrawingSnapshot(update));
	}

	@Override
	public Optional<MapDrawingUpdateResult> findMapDrawing(UUID tripId, UUID drawingId) {
		return Optional.ofNullable(mapper.findMapDrawing(tripId, drawingId));
	}

	@Override
	public boolean existsDay(UUID tripId, UUID dayId) {
		return mapper.existsDay(tripId, dayId);
	}

	@Override
	public long countDays(UUID tripId) {
		return mapper.countDays(tripId);
	}

	@Override
	public boolean existsItem(UUID tripId, UUID itemId) {
		return mapper.existsItem(tripId, itemId);
	}

	@Override
	public java.util.List<UUID> findActiveRouteIdsByItem(UUID tripId, UUID itemId) {
		return mapper.findActiveRouteIdsByItem(tripId, itemId);
	}

	@Override
	public boolean softDeleteItem(UUID tripId, UUID itemId, UUID deletedByUserId, Instant deletedAt) {
		return mapper.softDeleteItem(tripId, itemId, deletedByUserId, deletedAt) > 0;
	}

	@Override
	public boolean restoreItem(UUID tripId, UUID itemId, UUID updatedByUserId, Instant updatedAt) {
		return mapper.restoreItem(tripId, itemId, updatedByUserId, updatedAt) > 0;
	}

	@Override
	public boolean restoreMapDrawing(UUID tripId, UUID drawingId, UUID updatedByUserId, Instant updatedAt) {
		return mapper.restoreMapDrawing(tripId, drawingId, updatedByUserId, updatedAt) > 0;
	}

	@Override
	public boolean restoreRouteSegment(UUID tripId, UUID routeId, UUID updatedByUserId, Instant updatedAt) {
		return mapper.restoreRouteSegment(tripId, routeId, updatedByUserId, updatedAt) > 0;
	}

	@Override
	public long countActiveItems(UUID tripId) {
		return mapper.countActiveItems(tripId);
	}

	@Override
	public void updateDayOrder(ItineraryDayOrderUpdate update) {
		mapper.updateDayOrder(update);
	}

	@Override
	public void updateItemOrder(ItineraryItemOrderUpdate update) {
		mapper.updateItemOrder(update);
	}

	private ItineraryItemReadModel toItemReadModel(ItineraryItemRow row) {
		return new ItineraryItemReadModel(
			row.id(),
			row.itineraryDayId(),
			row.sortOrder(),
			ItineraryItemType.valueOf(row.itemType()),
			row.placeProvider(),
			row.externalPlaceId(),
			row.placeName(),
			row.address(),
			toDouble(row.lat()),
			toDouble(row.lng()),
			toUri(row.thumbnailUrl()),
			row.sourceStatus()
		);
	}

	private Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	private URI toUri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}
}
