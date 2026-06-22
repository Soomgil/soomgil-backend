package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryDayView;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.ReorderItineraryCommand;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.DeleteItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.ReorderItineraryHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryDayDetailView;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.util.List;
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
			itineraryHandler, dayHandler, itemHandler,
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class),
			mock(ReorderItineraryHandler.class)
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

	private AiItineraryToolService service(
		FindItineraryHandler itineraryHandler,
		ReorderItineraryHandler reorderHandler
	) {
		return new AiItineraryToolService(
			itineraryHandler, mock(CreateItineraryDayHandler.class), mock(CreateItineraryItemHandler.class),
			mock(DeleteItineraryItemHandler.class), mock(UpdateItineraryItemHandler.class), reorderHandler
		);
	}
}
