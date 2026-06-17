package com.soomgil.itinerary.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.MapDrawingView;
import com.soomgil.itinerary.application.command.dto.RouteSegmentView;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryQueryRepository;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryDayDetailView;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FindItineraryQuery}를 처리해 여행방 itinerary snapshot을 조회한다.
 */
@Component
public class FindItineraryHandler implements QueryHandler<FindItineraryQuery, ItineraryView> {

	private final TripAccessGuard tripAccessGuard;
	private final ItineraryQueryRepository repository;

	public FindItineraryHandler(TripAccessGuard tripAccessGuard, ItineraryQueryRepository repository) {
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public ItineraryView handle(FindItineraryQuery query) {
		tripAccessGuard.requireActiveMember(query.tripId(), query.userId());
		long version = repository.findItineraryVersion(query.tripId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found."));
		Map<UUID, List<ItineraryItemView>> itemsByDayId = repository.findItems(query.tripId())
			.stream()
			.map(this::toItemView)
			.collect(Collectors.groupingBy(ItineraryItemView::itineraryDayId));
		List<ItineraryDayDetailView> days = repository.findDays(query.tripId())
			.stream()
			.map(day -> new ItineraryDayDetailView(
				day.id(),
				day.tripId(),
				day.groupType(),
				day.dayNumber(),
				day.date(),
				day.title(),
				day.sortOrder(),
				itemsByDayId.getOrDefault(day.id(), List.of())
			))
			.toList();
		List<RouteSegmentView> routes = repository.findRoutes(query.tripId())
			.stream()
			.map(route -> new RouteSegmentView(
				route.id(),
				route.originItineraryItemId(),
				route.destinationItineraryItemId(),
				route.mode(),
				route.provider(),
				route.providerProfile(),
				route.geometryFormat(),
				route.geometry(),
				route.distanceMeters(),
				route.durationSeconds(),
				route.confidence()
			))
			.toList();
		List<MapDrawingView> mapDrawings = repository.findMapDrawings(query.tripId())
			.stream()
			.map(drawing -> new MapDrawingView(
				drawing.id(),
				drawing.itineraryDayId(),
				drawing.drawingType(),
				drawing.geometryFormat(),
				drawing.geometry(),
				drawing.style(),
				drawing.label(),
				drawing.sortOrder(),
				drawing.version()
			))
			.toList();
		return new ItineraryView(query.tripId(), version, days, routes, mapDrawings);
	}

	private ItineraryItemView toItemView(ItineraryItemReadModel item) {
		return new ItineraryItemView(
			item.id(),
			item.itineraryDayId(),
			item.sortOrder(),
			item.itemType(),
			item.placeProvider(),
			item.externalPlaceId(),
			item.placeName(),
			item.address(),
			item.lat(),
			item.lng(),
			item.thumbnailUrl(),
			item.sourceStatus()
		);
	}
}
