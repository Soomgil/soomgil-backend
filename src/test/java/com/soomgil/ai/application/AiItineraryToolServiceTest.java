package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryDayView;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.MapMatchRouteCommand;
import com.soomgil.itinerary.application.command.dto.MapMatchRouteResult;
import com.soomgil.itinerary.application.command.dto.ReorderItineraryCommand;
import com.soomgil.itinerary.application.command.dto.RouteSegmentView;
import com.soomgil.itinerary.application.command.dto.UpdateRouteSegmentCommand;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.DeleteItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.MapMatchRouteHandler;
import com.soomgil.itinerary.application.command.handler.ReorderItineraryHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.UpdateRouteSegmentHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryDayDetailView;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiItineraryToolServiceTest {

	@Test
	void movesPlaceByNameToTheRequestedDayNumber() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID sourceDayId = UUID.randomUUID();
		UUID targetDayId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		ReorderItineraryHandler reorderHandler = mock(ReorderItineraryHandler.class);
		ItineraryItemView item = new ItineraryItemView(
			itemId, sourceDayId, 0, ItineraryItemType.PLACE, "KTO", "place-1",
			"경복궁", "서울", 37.58, 126.97, null, "ACTIVE"
		);
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L,
			List.of(
				new ItineraryDayDetailView(
					sourceDayId, tripId, ItineraryDayGroupType.DAY, 1, null, "1일차", 0, List.of(item)
				),
				new ItineraryDayDetailView(
					targetDayId, tripId, ItineraryDayGroupType.DAY, 3, null, "3일차", 1, List.of()
				)
			), List.of(), List.of()
		));
		when(reorderHandler.handle(any())).thenReturn(new ItineraryMutationResult(
			tripId, 8L, null, null, null, null, List.of()
		));
		AiItineraryToolService service = service(itineraryHandler, reorderHandler);

		service.moveItem(tripId, userId, 7L, null, "경복궁", null, 3, null);

		ArgumentCaptor<ReorderItineraryCommand> captor = ArgumentCaptor.forClass(ReorderItineraryCommand.class);
		verify(reorderHandler).handle(captor.capture());
		assertThat(captor.getValue().days())
			.filteredOn(day -> day.dayId().equals(targetDayId))
			.singleElement()
			.satisfies(day -> assertThat(day.itemOrders()).extracting(order -> order.itemId())
				.containsExactly(itemId));
	}

	@Test
	void createsAnUnscheduledDayAndUsesItsNewVersionWhenNoDayWasSelected() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID dayId = UUID.randomUUID();
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		CreateItineraryDayHandler dayHandler = mock(CreateItineraryDayHandler.class);
		CreateItineraryItemHandler itemHandler = mock(CreateItineraryItemHandler.class);
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L, List.of(), List.of(), List.of()
		));
		CreateItineraryDayCommand dayCommand = new CreateItineraryDayCommand(
			tripId, userId, 7L, ItineraryDayGroupType.UNSCHEDULED, null, null, "일차 미정", 0
		);
		when(dayHandler.handle(dayCommand)).thenReturn(new ItineraryMutationResult(
			tripId, 8L, new ItineraryDayView(
				dayId, tripId, ItineraryDayGroupType.UNSCHEDULED, null, null, "일차 미정", 0
			), null, null, null, List.of()
		));

		new AiItineraryToolService(
			itineraryHandler, dayHandler, mock(UpdateItineraryDayHandler.class), itemHandler,
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			mock(ReorderItineraryHandler.class), mock(MapMatchRouteHandler.class),
			mock(UpdateRouteSegmentHandler.class)
		).addPlace(
			tripId, userId, 7L, new AiItineraryToolService.AddPlaceInput(
				null, 0, "KTO", "place-1", "협재해수욕장", "제주시", 33.39, 126.24, null
			)
		);

		verify(dayHandler).handle(dayCommand);
		verify(itemHandler).handle(new CreateItineraryItemCommand(
			tripId, userId, 8L, dayId, 0, ItineraryItemType.PLACE,
			"KTO", "place-1", "협재해수욕장", "제주시", 33.39, 126.24, null
		));
	}

	@Test
	void connectsAdjacentItemsInTheRequestedDayWithCyclingMode() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID dayId = UUID.randomUUID();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		MapMatchRouteHandler mapMatchRouteHandler = mock(MapMatchRouteHandler.class);
		ItineraryItemView first = item(firstId, dayId, 0, "성산일출봉", 33.458, 126.942);
		ItineraryItemView second = item(secondId, dayId, 1, "섭지코지", 33.424, 126.930);
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L,
			List.of(new ItineraryDayDetailView(
				dayId, tripId, ItineraryDayGroupType.DAY, 2, null, "2일차", 0, List.of(first, second)
			)),
			List.of(),
			List.of()
		));
		RouteSegmentView route = route(UUID.randomUUID(), firstId, secondId, RouteMode.CYCLING);
		when(mapMatchRouteHandler.handle(any())).thenReturn(new MapMatchRouteResult(
			new ItineraryMutationResult(tripId, 8L, null, null, route, null, List.of(route.id())),
			1L,
			List.of(),
			Map.of()
		));
		AiItineraryToolService service = new AiItineraryToolService(
			itineraryHandler, mock(CreateItineraryDayHandler.class), mock(UpdateItineraryDayHandler.class),
			mock(CreateItineraryItemHandler.class),
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			mock(ReorderItineraryHandler.class), mapMatchRouteHandler, mock(UpdateRouteSegmentHandler.class)
		);

		AiItineraryToolService.ConnectDayRoutesResult result = service.connectDayRoutes(
			tripId, userId, 7L, null, 2, RouteMode.CYCLING
		);

		ArgumentCaptor<MapMatchRouteCommand> captor = ArgumentCaptor.forClass(MapMatchRouteCommand.class);
		verify(mapMatchRouteHandler).handle(captor.capture());
		assertThat(captor.getValue().mode()).isEqualTo(RouteMode.CYCLING);
		assertThat(captor.getValue().originItineraryItemId()).isEqualTo(firstId);
		assertThat(captor.getValue().destinationItineraryItemId()).isEqualTo(secondId);
		assertThat(captor.getValue().coordinates()).extracting(coordinate -> coordinate.lng())
			.containsExactly(126.942, 126.930);
		assertThat(result.createdCount()).isEqualTo(1);
		assertThat(result.versionAfter()).isEqualTo(8L);
	}

	@Test
	void updatesExistingAdjacentRouteWhenTheRequestedModeDiffers() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID dayId = UUID.randomUUID();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		UUID routeId = UUID.randomUUID();
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		UpdateRouteSegmentHandler updateRouteSegmentHandler = mock(UpdateRouteSegmentHandler.class);
		ItineraryItemView first = item(firstId, dayId, 0, "성산일출봉", 33.458, 126.942);
		ItineraryItemView second = item(secondId, dayId, 1, "섭지코지", 33.424, 126.930);
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L,
			List.of(new ItineraryDayDetailView(
				dayId, tripId, ItineraryDayGroupType.DAY, 2, null, "2일차", 0, List.of(first, second)
			)),
			List.of(route(routeId, firstId, secondId, RouteMode.WALKING)),
			List.of()
		));
		when(updateRouteSegmentHandler.handle(any())).thenReturn(new ItineraryMutationResult(
			tripId, 8L, null, null, route(routeId, firstId, secondId, RouteMode.CYCLING), null, List.of(routeId)
		));
		AiItineraryToolService service = new AiItineraryToolService(
			itineraryHandler, mock(CreateItineraryDayHandler.class), mock(UpdateItineraryDayHandler.class),
			mock(CreateItineraryItemHandler.class),
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			mock(ReorderItineraryHandler.class), mock(MapMatchRouteHandler.class), updateRouteSegmentHandler
		);

		AiItineraryToolService.ConnectDayRoutesResult result = service.connectDayRoutes(
			tripId, userId, 7L, null, 2, RouteMode.CYCLING
		);

		verify(updateRouteSegmentHandler).handle(new UpdateRouteSegmentCommand(
			tripId, userId, 7L, routeId, RouteMode.CYCLING, null, null, null, null
		));
		assertThat(result.updatedCount()).isEqualTo(1);
		assertThat(result.versionAfter()).isEqualTo(8L);
	}

	@Test
	void createDayAssignsTheNextDayNumberWhenTheCallerDoesNotSpecifyOne() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		CreateItineraryDayHandler dayHandler = mock(CreateItineraryDayHandler.class);
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L,
			List.of(
				day(tripId, 1), day(tripId, 2),
				new ItineraryDayDetailView(
					UUID.randomUUID(), tripId, ItineraryDayGroupType.UNSCHEDULED,
					null, null, "일차 미정", 0, List.of()
				)
			),
			List.of(), List.of()
		));
		when(dayHandler.handle(any(CreateItineraryDayCommand.class))).thenReturn(new ItineraryMutationResult(
			tripId, 8L, null, null, null, null, List.of()
		));

		new AiItineraryToolService(
			itineraryHandler, dayHandler, mock(UpdateItineraryDayHandler.class),
			mock(CreateItineraryItemHandler.class),
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			mock(ReorderItineraryHandler.class), mock(MapMatchRouteHandler.class),
			mock(UpdateRouteSegmentHandler.class)
		).createDay(tripId, userId, 7L, null, null, null);

		ArgumentCaptor<CreateItineraryDayCommand> captor = ArgumentCaptor.forClass(CreateItineraryDayCommand.class);
		verify(dayHandler).handle(captor.capture());
		assertThat(captor.getValue().dayNumber()).isEqualTo(3);
		assertThat(captor.getValue().groupType()).isEqualTo(ItineraryDayGroupType.DAY);
		assertThat(captor.getValue().title()).isEqualTo("3일차");
	}

	@Test
	void createDayRejectsADayNumberThatAlreadyExists() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		CreateItineraryDayHandler dayHandler = mock(CreateItineraryDayHandler.class);
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L, List.of(day(tripId, 1), day(tripId, 2)), List.of(), List.of()
		));
		AiItineraryToolService service = new AiItineraryToolService(
			itineraryHandler, dayHandler, mock(UpdateItineraryDayHandler.class),
			mock(CreateItineraryItemHandler.class),
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			mock(ReorderItineraryHandler.class), mock(MapMatchRouteHandler.class),
			mock(UpdateRouteSegmentHandler.class)
		);

		assertThatThrownBy(() -> service.createDay(tripId, userId, 7L, 2, null, null))
			.isInstanceOf(com.soomgil.global.error.BusinessException.class);
		verify(dayHandler, never()).handle(any(CreateItineraryDayCommand.class));
	}

	private ItineraryDayDetailView day(UUID tripId, int dayNumber) {
		return new ItineraryDayDetailView(
			UUID.randomUUID(), tripId, ItineraryDayGroupType.DAY,
			dayNumber, null, dayNumber + "일차", dayNumber, List.of()
		);
	}

	private AiItineraryToolService service(
		FindItineraryHandler itineraryHandler,
		ReorderItineraryHandler reorderHandler
	) {
		return new AiItineraryToolService(
			itineraryHandler, mock(CreateItineraryDayHandler.class), mock(UpdateItineraryDayHandler.class),
			mock(CreateItineraryItemHandler.class),
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class), reorderHandler,
			mock(MapMatchRouteHandler.class), mock(UpdateRouteSegmentHandler.class)
		);
	}

	private ItineraryItemView item(UUID itemId, UUID dayId, int sortOrder, String name, double lat, double lng) {
		return new ItineraryItemView(
			itemId, dayId, sortOrder, ItineraryItemType.PLACE, "KTO", itemId.toString(),
			name, "제주", lat, lng, null, "ACTIVE"
		);
	}

	private RouteSegmentView route(UUID routeId, UUID originId, UUID destinationId, RouteMode mode) {
		return new RouteSegmentView(
			routeId, originId, destinationId, mode, "MAPBOX", "mapbox/" + mode.name().toLowerCase(),
			GeometryFormat.GEOJSON,
			Map.of("type", "LineString", "coordinates", List.of(List.of(126.0, 33.0), List.of(126.1, 33.1))),
			1000.0,
			600.0,
			0.9
		);
	}
}
