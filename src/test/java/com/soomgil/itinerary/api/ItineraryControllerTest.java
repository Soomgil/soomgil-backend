package com.soomgil.itinerary.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.itinerary.api.dto.CreateItineraryDayRequest;
import com.soomgil.itinerary.api.dto.CreateItineraryItemRequest;
import com.soomgil.itinerary.api.dto.CreateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.DrawingType;
import com.soomgil.itinerary.api.dto.ItineraryDayGroupType;
import com.soomgil.itinerary.api.dto.ItineraryDayOrder;
import com.soomgil.itinerary.api.dto.ItineraryItemType;
import com.soomgil.itinerary.api.dto.ItineraryItemOrder;
import com.soomgil.itinerary.api.dto.ItineraryMutationResponse;
import com.soomgil.itinerary.api.dto.MapMatchRouteRequest;
import com.soomgil.itinerary.api.dto.MapMatchRouteResponse;
import com.soomgil.itinerary.api.dto.ReorderItineraryRequest;
import com.soomgil.itinerary.api.dto.RouteMode;
import com.soomgil.itinerary.api.dto.UpdateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.UpdateItineraryDayRequest;
import com.soomgil.itinerary.api.dto.UpdateItineraryItemRequest;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.CreateMapDrawingHandler;
import com.soomgil.itinerary.application.command.handler.DeleteItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.DeleteMapDrawingHandler;
import com.soomgil.itinerary.application.command.handler.DeleteItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.DeleteRouteSegmentHandler;
import com.soomgil.itinerary.application.command.handler.MapMatchRouteHandler;
import com.soomgil.itinerary.application.command.handler.ReorderItineraryHandler;
import com.soomgil.itinerary.application.command.handler.SaveRouteSegmentHandler;
import com.soomgil.itinerary.application.command.handler.UpdateMapDrawingHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.UpdateRouteSegmentHandler;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemUpdate;
import com.soomgil.itinerary.application.port.ItineraryQueryRepository;
import com.soomgil.itinerary.application.port.MapMatchClientRequest;
import com.soomgil.itinerary.application.port.MapMatchClientResult;
import com.soomgil.itinerary.application.port.MapMatchingClient;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingReadModel;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentReadModel;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdateResult;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.geo.api.dto.LngLat;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripInviteAcceptReadModel;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ItineraryControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID ROUTE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
	private static final UUID DRAWING_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
	private static final UUID ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	@Test
	void createsDayResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.createDay(
			TRIP_ID,
			new CreateItineraryDayRequest(0L, ItineraryDayGroupType.DAY, 1, LocalDate.parse("2026-07-01"), "1일차", 0),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.day().dayNumber()).isEqualTo(1);
	}

	@Test
	void getsItineraryResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		com.soomgil.itinerary.api.dto.Itinerary result = controller.getItinerary(TRIP_ID, principal());

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(7);
		assertThat(result.days()).hasSize(1);
		assertThat(result.days().get(0).items()).hasSize(1);
		assertThat(result.routes()).hasSize(1);
		assertThat(result.mapDrawings()).hasSize(1);
	}

	@Test
	void updatesDayResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.updateDay(
			TRIP_ID,
			DAY_ID,
			new UpdateItineraryDayRequest(0L, 2, LocalDate.parse("2026-07-02"), "둘째 날", 2),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.day().id()).isEqualTo(DAY_ID);
		assertThat(result.day().dayNumber()).isEqualTo(2);
		assertThat(result.day().title()).isEqualTo("둘째 날");
	}

	@Test
	void deletesDayResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.deleteDay(
			TRIP_ID,
			DAY_ID,
			new com.soomgil.collaboration.api.dto.VersionedCommandRequest(0L),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(repository.deletedDayId).isEqualTo(DAY_ID);
	}

	@Test
	void deletesItemResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.deleteItem(
			TRIP_ID,
			ITEM_ID,
			new com.soomgil.collaboration.api.dto.VersionedCommandRequest(0L),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.affectedRouteIds()).containsExactly(ROUTE_ID);
		assertThat(repository.deletedItemId).isEqualTo(ITEM_ID);
	}

	@Test
	void updatesItemResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.updateItem(
			TRIP_ID,
			ITEM_ID,
			new UpdateItineraryItemRequest(
				0L,
				null,
				null,
				"수정 장소",
				"새 주소",
				36.1,
				127.1,
				null
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.item().id()).isEqualTo(ITEM_ID);
		assertThat(result.item().placeName()).isEqualTo("수정 장소");
		assertThat(result.item().sortOrder()).isEqualTo(0);
	}

	@Test
	void createsItemResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.createItem(
			TRIP_ID,
			new CreateItineraryItemRequest(
				0L,
				DAY_ID,
				0,
				ItineraryItemType.PLACE,
				new PlaceRef(PlaceProvider.KTO, "126508"),
				"성심당",
				null,
				null,
				null,
				null
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.item().place().provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(result.item().placeName()).isEqualTo("성심당");
	}

	@Test
	void reordersItineraryResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.reorderItinerary(
			TRIP_ID,
			new ReorderItineraryRequest(
				0L,
				List.of(new ItineraryDayOrder(
					DAY_ID,
					0,
					List.of(new ItineraryItemOrder(UUID.fromString("40000000-0000-0000-0000-000000000001"), 1))
				))
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(repository.itemOrderUpdated).isTrue();
	}

	@Test
	void createsMapDrawingResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.createDrawing(
			TRIP_ID,
			new CreateMapDrawingRequest(
				0L,
				DAY_ID,
				DrawingType.LINE,
				java.util.Map.of("type", "LineString", "coordinates", List.of(List.of(127.0, 37.0), List.of(128.0, 38.0))),
				java.util.Map.of("color", "#111111"),
				"이동선",
				2
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.drawing().drawingType()).isEqualTo(DrawingType.LINE);
		assertThat(repository.insertedDrawing.label()).isEqualTo("이동선");
	}

	@Test
	void mapMatchesRouteResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);
		UUID originItemId = UUID.fromString("40000000-0000-0000-0000-000000000001");
		UUID destinationItemId = UUID.fromString("40000000-0000-0000-0000-000000000002");

		MapMatchRouteResponse result = controller.mapMatchRoute(
			TRIP_ID,
			new MapMatchRouteRequest(
				0L,
				originItemId,
				destinationItemId,
				RouteMode.WALKING,
				List.of(new LngLat(127.0, 37.0), new LngLat(127.1, 37.1)),
				null,
				true
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.route().provider()).isEqualTo("MAPBOX");
		assertThat(result.route().providerProfile()).isEqualTo("mapbox/walking");
		assertThat(result.matchRequestId()).isEqualTo(10L);
		assertThat(result.matchingsMetadata()).containsEntry("code", "Ok");
	}

	@Test
	void deletesRouteResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.deleteRoute(
			TRIP_ID,
			ROUTE_ID,
			new com.soomgil.collaboration.api.dto.VersionedCommandRequest(0L),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.affectedRouteIds()).containsExactly(ROUTE_ID);
		assertThat(repository.deletedRouteId).isEqualTo(ROUTE_ID);
	}

	@Test
	void updatesRouteResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.updateRoute(
			TRIP_ID,
			ROUTE_ID,
			new com.soomgil.itinerary.api.dto.UpdateRouteRequest(
				0L,
				RouteMode.WALKING,
				java.util.Map.of("type", "LineString"),
				200.0,
				120.0,
				0.95
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.route().id()).isEqualTo(ROUTE_ID);
		assertThat(result.route().mode()).isEqualTo(RouteMode.WALKING);
		assertThat(repository.updatedRouteId).isEqualTo(ROUTE_ID);
	}

	@Test
	void deletesMapDrawingResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.deleteDrawing(
			TRIP_ID,
			DRAWING_ID,
			new com.soomgil.collaboration.api.dto.VersionedCommandRequest(0L),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.affectedRouteIds()).isEmpty();
		assertThat(repository.deletedDrawingId).isEqualTo(DRAWING_ID);
	}

	@Test
	void updatesMapDrawingResponse() {
		StubItineraryCommandRepository repository = new StubItineraryCommandRepository();
		ItineraryController controller = controller(repository);

		ItineraryMutationResponse result = controller.updateDrawing(
			TRIP_ID,
			DRAWING_ID,
			new UpdateMapDrawingRequest(
				0L,
				java.util.Map.of("type", "LineString"),
				java.util.Map.of("color", "#222222"),
				"수정된 선",
				2,
				0L
			),
			principal()
		);

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.drawing().id()).isEqualTo(DRAWING_ID);
		assertThat(result.drawing().version()).isEqualTo(1);
		assertThat(result.drawing().label()).isEqualTo("수정된 선");
	}

	private ItineraryController controller(StubItineraryCommandRepository repository) {
		CapturingEventRepository eventRepository = new CapturingEventRepository();
		return new ItineraryController(
			new CreateItineraryDayHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new CreateItineraryItemHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new ReorderItineraryHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new CreateMapDrawingHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z"),
				new ObjectMapper()
			),
			new MapMatchRouteHandler(
				repository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				new StubMapMatchingClient(),
				new SaveRouteSegmentHandler(
					repository,
					eventRepository,
					new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
					() -> Instant.parse("2026-06-17T00:00:00Z"),
					new ObjectMapper()
				),
				() -> Instant.parse("2026-06-17T00:00:00Z"),
				new ObjectMapper()
			),
			new FindItineraryHandler(
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				new StubItineraryQueryRepository()
			),
			new DeleteRouteSegmentHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new DeleteMapDrawingHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new UpdateMapDrawingHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z"),
				new ObjectMapper()
			),
			new UpdateItineraryDayHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new DeleteItineraryItemHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new UpdateItineraryItemHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new DeleteItineraryDayHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z")
			),
			new UpdateRouteSegmentHandler(
				repository,
				eventRepository,
				new com.soomgil.trip.application.query.handler.TripAccessGuard(new StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z"),
				new ObjectMapper()
			)
		);
	}

	private static class StubItineraryQueryRepository implements ItineraryQueryRepository {

		@Override
		public OptionalLong findItineraryVersion(UUID tripId) {
			return OptionalLong.of(7);
		}

		@Override
		public List<ItineraryDayReadModel> findDays(UUID tripId) {
			return List.of(new ItineraryDayReadModel(
				DAY_ID,
				TRIP_ID,
				com.soomgil.itinerary.domain.model.ItineraryDayGroupType.DAY,
				1,
				LocalDate.parse("2026-07-01"),
				"1일차",
				0
			));
		}

		@Override
		public List<ItineraryItemReadModel> findItems(UUID tripId) {
			return List.of(new ItineraryItemReadModel(
				UUID.fromString("40000000-0000-0000-0000-000000000001"),
				DAY_ID,
				0,
				com.soomgil.itinerary.domain.model.ItineraryItemType.PLACE,
				"KTO",
				"126508",
				"성심당",
				"대전",
				36.327,
				127.427,
				null,
				"AVAILABLE"
			));
		}

		@Override
		public List<RouteSegmentReadModel> findRoutes(UUID tripId) {
			return List.of(new RouteSegmentReadModel(
				UUID.fromString("50000000-0000-0000-0000-000000000001"),
				UUID.fromString("40000000-0000-0000-0000-000000000001"),
				UUID.fromString("40000000-0000-0000-0000-000000000002"),
				com.soomgil.itinerary.domain.model.RouteMode.WALKING,
				"MAPBOX",
				"mapbox/walking",
				com.soomgil.itinerary.domain.model.GeometryFormat.GEOJSON,
				java.util.Map.of("type", "LineString"),
				120.0,
				60.0,
				0.98
			));
		}

		@Override
		public List<MapDrawingReadModel> findMapDrawings(UUID tripId) {
			return List.of(new MapDrawingReadModel(
				UUID.fromString("60000000-0000-0000-0000-000000000001"),
				DAY_ID,
				com.soomgil.itinerary.domain.model.DrawingType.LINE,
				com.soomgil.itinerary.domain.model.GeometryFormat.GEOJSON,
				java.util.Map.of("type", "LineString"),
				java.util.Map.of("color", "#111111"),
				"이동선",
				0,
				1L
			));
		}
	}

	private static class StubMapMatchingClient implements MapMatchingClient {

		@Override
		public MapMatchClientResult match(MapMatchClientRequest request) {
			return new MapMatchClientResult(
				java.util.Map.of(
					"type",
					"LineString",
					"coordinates",
					List.of(List.of(127.0, 37.0), List.of(127.1, 37.1))
				),
				List.of(java.util.Map.of("name", "tracepoint")),
				java.util.Map.of("code", "Ok"),
				120.5,
				60.0,
				0.98
			);
		}
	}

	private static class CapturingEventRepository implements CollaborationCommandEventRepository {

		@Override
		public void save(CollaborationCommandEvent event) {
		}
	}

	private Principal principal() {
		return () -> USER_ID.toString();
	}

	private static class StubItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private boolean itemOrderUpdated;
		private MapDrawingCreate insertedDrawing;
		private UUID deletedRouteId;
		private UUID deletedDrawingId;
		private UUID deletedItemId;
		private UUID deletedDayId;
		private UUID updatedRouteId;

		@Override
		public OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt) {
			if (currentVersion != baseVersion) {
				return OptionalLong.empty();
			}
			currentVersion++;
			return OptionalLong.of(currentVersion);
		}

		@Override
		public OptionalLong findItineraryVersion(UUID tripId) {
			return OptionalLong.of(currentVersion);
		}

		@Override
		public void insertDay(ItineraryDayCreate day) {
		}

		@Override
		public Optional<ItineraryDayReadModel> findDay(UUID tripId, UUID dayId) {
			return Optional.of(new ItineraryDayReadModel(
				dayId,
				tripId,
				com.soomgil.itinerary.domain.model.ItineraryDayGroupType.DAY,
				1,
				LocalDate.parse("2026-07-01"),
				"첫째 날",
				0
			));
		}

		@Override
		public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public Optional<ItineraryDayReadModel> updateDay(com.soomgil.itinerary.application.port.ItineraryDayUpdate update) {
			return Optional.of(new ItineraryDayReadModel(
				update.dayId(),
				update.tripId(),
				com.soomgil.itinerary.domain.model.ItineraryDayGroupType.DAY,
				update.dayNumber(),
				update.date(),
				update.title(),
				update.sortOrder()
			));
		}

		@Override
		public long countActiveItemsByDay(UUID tripId, UUID dayId) {
			return 0;
		}

		@Override
		public boolean deleteDay(UUID tripId, UUID dayId) {
			this.deletedDayId = dayId;
			return true;
		}

		@Override
		public void insertItem(ItineraryItemCreate item) {
		}

		@Override
		public Optional<ItineraryItemReadModel> findItem(UUID tripId, UUID itemId) {
			return Optional.of(new ItineraryItemReadModel(
				itemId,
				DAY_ID,
				0,
				com.soomgil.itinerary.domain.model.ItineraryItemType.PLACE,
				"KTO",
				"126508",
				"성심당",
				"대전",
				36.0,
				127.0,
				null,
				"AVAILABLE"
			));
		}

		@Override
		public Optional<ItineraryItemReadModel> updateItem(ItineraryItemUpdate update) {
			return Optional.of(new ItineraryItemReadModel(
				update.itemId(),
				update.itineraryDayId(),
				update.sortOrder(),
				com.soomgil.itinerary.domain.model.ItineraryItemType.PLACE,
				"KTO",
				"126508",
				update.placeName(),
				update.address(),
				update.lat(),
				update.lng(),
				update.thumbnailUrl() == null ? null : java.net.URI.create(update.thumbnailUrl()),
				"AVAILABLE"
			));
		}

		@Override
		public void insertMapDrawing(MapDrawingCreate drawing) {
			this.insertedDrawing = drawing;
		}

		@Override
		public void insertRouteSegment(RouteSegmentCreate route) {
		}

		@Override
		public Optional<RouteSegmentUpdateResult> updateRouteSegment(RouteSegmentUpdate update) {
			this.updatedRouteId = update.routeId();
			return Optional.of(new RouteSegmentUpdateResult(
				update.routeId(),
				UUID.fromString("40000000-0000-0000-0000-000000000001"),
				UUID.fromString("40000000-0000-0000-0000-000000000002"),
				update.mode(),
				"MAPBOX",
				update.providerProfile(),
				com.soomgil.itinerary.domain.model.GeometryFormat.GEOJSON,
				update.geometry(),
				update.distanceMeters(),
				update.durationSeconds(),
				update.confidence()
			));
		}

		@Override
		public Long insertRouteMatchRequest(RouteMatchRequestLog request) {
			return 10L;
		}

		@Override
		public boolean existsActiveRouteSegment(UUID tripId, UUID routeId) {
			return true;
		}

		@Override
		public boolean softDeleteRouteSegment(UUID tripId, UUID routeId, UUID deletedByUserId, Instant deletedAt) {
			this.deletedRouteId = routeId;
			return true;
		}

		@Override
		public boolean existsActiveMapDrawing(UUID tripId, UUID drawingId) {
			return true;
		}

		@Override
		public boolean softDeleteMapDrawing(UUID tripId, UUID drawingId, UUID deletedByUserId, Instant deletedAt) {
			this.deletedDrawingId = drawingId;
			return true;
		}

		@Override
		public Optional<MapDrawingUpdateResult> updateMapDrawing(MapDrawingUpdate update) {
			return Optional.of(new MapDrawingUpdateResult(
				update.drawingId(),
				DAY_ID,
				com.soomgil.itinerary.domain.model.DrawingType.LINE,
				com.soomgil.itinerary.domain.model.GeometryFormat.GEOJSON,
				update.geometry(),
				update.style(),
				update.label(),
				update.sortOrder(),
				1L
			));
		}

		@Override
		public boolean existsDay(UUID tripId, UUID dayId) {
			return true;
		}

		@Override
		public long countDays(UUID tripId) {
			return 1;
		}

		@Override
		public boolean existsItem(UUID tripId, UUID itemId) {
			return true;
		}

		@Override
		public List<UUID> findActiveRouteIdsByItem(UUID tripId, UUID itemId) {
			return List.of(ROUTE_ID);
		}

		@Override
		public boolean softDeleteItem(UUID tripId, UUID itemId, UUID deletedByUserId, Instant deletedAt) {
			this.deletedItemId = itemId;
			return true;
		}

		@Override
		public long countActiveItems(UUID tripId) {
			return 1;
		}

		@Override
		public void updateDayOrder(ItineraryDayOrderUpdate update) {
		}

		@Override
		public void updateItemOrder(ItineraryItemOrderUpdate update) {
			this.itemOrderUpdated = true;
		}
	}

	private static class StubTripQueryRepository implements TripQueryRepository {

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return Optional.of(new TripAccessSnapshot(
				tripId,
				userId,
				TripStatus.ACTIVE,
				TripMemberStatus.ACTIVE,
				userId
			));
		}

		@Override
		public Optional<TripReadModel> findTrip(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public List<TripMemberReadModel> findTripMembers(UUID tripId, TripMemberStatus status) {
			return List.of();
		}

		@Override
		public TripSummaryPage findMyTrips(
			UUID userId,
			TripStatus status,
			TripAccessRole role,
			int page,
			int size,
			List<String> sort
		) {
			return new TripSummaryPage(List.of(), 0);
		}

		@Override
		public List<TripInviteReadModel> findTripInvites(UUID tripId, InviteStatus status) {
			return List.of();
		}

		@Override
		public Optional<TripInviteAcceptReadModel> findTripInviteForAccept(String inviteCode) {
			return Optional.empty();
		}
	}
}
