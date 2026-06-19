package com.soomgil.itinerary.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryQueryRepository;
import com.soomgil.itinerary.application.port.MapDrawingReadModel;
import com.soomgil.itinerary.application.port.RouteSegmentReadModel;
import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.itinerary.infrastructure.persistence.mapper.ItineraryQueryMapper;
import com.soomgil.itinerary.infrastructure.persistence.row.ItineraryItemRow;
import com.soomgil.itinerary.infrastructure.persistence.row.MapDrawingRow;
import com.soomgil.itinerary.infrastructure.persistence.row.RouteSegmentRow;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 itinerary 읽기 repository.
 */
@Repository
public class MyBatisItineraryQueryRepository implements ItineraryQueryRepository {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final ItineraryQueryMapper mapper;
	private final ObjectMapper objectMapper;

	public MyBatisItineraryQueryRepository(ItineraryQueryMapper mapper, ObjectMapper objectMapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public OptionalLong findItineraryVersion(UUID tripId) {
		Long version = mapper.findItineraryVersion(tripId);
		return version == null ? OptionalLong.empty() : OptionalLong.of(version);
	}

	@Override
	public List<ItineraryDayReadModel> findDays(UUID tripId) {
		return mapper.findDays(tripId);
	}

	@Override
	public List<ItineraryItemReadModel> findItems(UUID tripId) {
		return mapper.findItems(tripId).stream()
			.map(this::toItemReadModel)
			.toList();
	}

	@Override
	public List<RouteSegmentReadModel> findRoutes(UUID tripId) {
		return mapper.findRoutes(tripId).stream()
			.map(this::toRouteReadModel)
			.toList();
	}

	@Override
	public List<MapDrawingReadModel> findMapDrawings(UUID tripId) {
		return mapper.findMapDrawings(tripId).stream()
			.map(this::toDrawingReadModel)
			.toList();
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

	private RouteSegmentReadModel toRouteReadModel(RouteSegmentRow row) {
		return new RouteSegmentReadModel(
			row.id(),
			row.originItineraryItemId(),
			row.destinationItineraryItemId(),
			RouteMode.valueOf(row.mode()),
			row.provider(),
			row.providerProfile(),
			GeometryFormat.valueOf(row.geometryFormat()),
			toMap(row.geometry()),
			toDouble(row.distanceMeters()),
			toDouble(row.durationSeconds()),
			toDouble(row.confidence())
		);
	}

	private MapDrawingReadModel toDrawingReadModel(MapDrawingRow row) {
		return new MapDrawingReadModel(
			row.id(),
			row.itineraryDayId(),
			DrawingType.valueOf(row.drawingType()),
			GeometryFormat.valueOf(row.geometryFormat()),
			toMap(row.geometry()),
			row.style() == null ? null : toMap(row.style()),
			row.label(),
			row.sortOrder(),
			row.version()
		);
	}

	private Map<String, Object> toMap(String json) {
		try {
			return objectMapper.readValue(json, MAP_TYPE);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Itinerary JSON column is invalid.", exception);
		}
	}

	private Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	private URI toUri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}
}
