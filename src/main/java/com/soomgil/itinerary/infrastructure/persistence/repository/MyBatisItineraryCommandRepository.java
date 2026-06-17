package com.soomgil.itinerary.infrastructure.persistence.repository;

import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.infrastructure.persistence.mapper.ItineraryCommandMapper;
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
	public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
		return Optional.ofNullable(mapper.findUnscheduledDay(tripId));
	}

	@Override
	public void insertItem(ItineraryItemCreate item) {
		mapper.insertItem(item);
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
}
