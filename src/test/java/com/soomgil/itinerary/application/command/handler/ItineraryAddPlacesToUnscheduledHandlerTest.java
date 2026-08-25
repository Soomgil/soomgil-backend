package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledCommand;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledResult;
import com.soomgil.itinerary.application.command.dto.UnscheduledPlaceToAdd;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryQueryRepository;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ItineraryAddPlacesToUnscheduledHandlerTest {

	private static final Instant NOW = Instant.parse("2026-08-24T09:00:00Z");

	private final ItineraryCommandRepository commandRepository = mock(ItineraryCommandRepository.class);
	private final ItineraryQueryRepository queryRepository = mock(ItineraryQueryRepository.class);
	private final CollaborationCommandEventRepository eventRepository =
		mock(CollaborationCommandEventRepository.class);
	private final TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);

	private final ItineraryAddPlacesToUnscheduledHandler handler = new ItineraryAddPlacesToUnscheduledHandler(
		commandRepository, queryRepository, eventRepository, tripAccessGuard, () -> NOW
	);

	private final UUID tripId = UUID.randomUUID();
	private final UUID actorId = UUID.randomUUID();
	private final UUID unscheduledDayId = UUID.randomUUID();

	@BeforeEach
	void stubCommonRepository() {
		when(commandRepository.findItineraryVersion(tripId)).thenReturn(OptionalLong.of(7L));
		when(commandRepository.incrementItineraryVersion(eq(tripId), anyLong(), any(Instant.class)))
			.thenReturn(OptionalLong.of(8L));
		when(queryRepository.findItems(tripId)).thenReturn(List.of());
		when(commandRepository.findUnscheduledDay(tripId)).thenReturn(Optional.of(unscheduledDay()));
	}

	@Test
	@DisplayName("일차 미정이 이미 있으면 재사용하고 새 day를 만들지 않는다")
	void reusesExistingUnscheduledDay() {
		AddPlacesToUnscheduledResult result = handler.handle(command(place("126508", "성산일출봉")));

		assertThat(result.unscheduledDayId()).isEqualTo(unscheduledDayId);
		assertThat(result.added()).hasSize(1);
		verify(commandRepository, never()).insertDay(any(ItineraryDayCreate.class));
	}

	@Test
	@DisplayName("일차 미정이 없으면 새로 만들고 거기에 추가한다")
	void createsUnscheduledDayWhenMissing() {
		when(commandRepository.findUnscheduledDay(tripId)).thenReturn(Optional.empty());
		when(commandRepository.countDays(tripId)).thenReturn(3L);

		AddPlacesToUnscheduledResult result = handler.handle(command(place("126508", "성산일출봉")));

		ArgumentCaptor<ItineraryDayCreate> dayCaptor = ArgumentCaptor.forClass(ItineraryDayCreate.class);
		verify(commandRepository).insertDay(dayCaptor.capture());
		assertThat(dayCaptor.getValue().groupType()).isEqualTo(ItineraryDayGroupType.UNSCHEDULED);
		assertThat(dayCaptor.getValue().dayNumber()).isNull();
		assertThat(result.unscheduledDayId()).isEqualTo(dayCaptor.getValue().id());
	}

	@Test
	@DisplayName("이미 일차 미정에 있는 장소는 중복 추가하지 않는다")
	void skipsPlaceAlreadyInUnscheduledDay() {
		UUID existingItemId = UUID.randomUUID();
		when(queryRepository.findItems(tripId)).thenReturn(List.of(
			item(existingItemId, unscheduledDayId, "KTO", "126508")
		));

		AddPlacesToUnscheduledResult result = handler.handle(command(place("126508", "성산일출봉")));

		assertThat(result.added()).isEmpty();
		assertThat(result.skippedDuplicates()).hasSize(1);
		assertThat(result.skippedDuplicates().get(0).existingItineraryItemId()).isEqualTo(existingItemId);
		verify(commandRepository, never()).insertItem(any(ItineraryItemCreate.class));
	}

	@Test
	@DisplayName("확정된 실제 day에 이미 있는 장소도 중복 추가하지 않는다")
	void skipsPlaceAlreadyInScheduledDay() {
		UUID scheduledDayId = UUID.randomUUID();
		when(queryRepository.findItems(tripId)).thenReturn(List.of(
			item(UUID.randomUUID(), scheduledDayId, "KTO", "126508")
		));

		AddPlacesToUnscheduledResult result = handler.handle(command(place("126508", "성산일출봉")));

		assertThat(result.added()).isEmpty();
		assertThat(result.skippedDuplicates()).hasSize(1);
	}

	@Test
	@DisplayName("추가할 장소가 모두 중복이면 itinerary version을 올리지 않는다")
	void doesNotBumpVersionWhenEverythingIsDuplicate() {
		when(queryRepository.findItems(tripId)).thenReturn(List.of(
			item(UUID.randomUUID(), unscheduledDayId, "KTO", "126508")
		));

		AddPlacesToUnscheduledResult result = handler.handle(command(place("126508", "성산일출봉")));

		assertThat(result.itineraryVersion()).isEqualTo(7L);
		verify(commandRepository, never()).incrementItineraryVersion(any(UUID.class), anyLong(), any(Instant.class));
	}

	@Test
	@DisplayName("여러 장소를 한 번에 추가해도 version은 한 번만 증가한다")
	void bumpsVersionOnceForBatch() {
		AddPlacesToUnscheduledResult result = handler.handle(command(
			place("126508", "성산일출봉"),
			place("126509", "우도"),
			place("126510", "협재해수욕장")
		));

		assertThat(result.added()).hasSize(3);
		assertThat(result.itineraryVersion()).isEqualTo(8L);
		verify(commandRepository, times(1))
			.incrementItineraryVersion(eq(tripId), anyLong(), any(Instant.class));
		verify(commandRepository, times(3)).insertItem(any(ItineraryItemCreate.class));
	}

	@Test
	@DisplayName("추가되는 아이템은 기존 마지막 순서 뒤에 이어 붙는다")
	void appendsAfterExistingSortOrder() {
		when(queryRepository.findItems(tripId)).thenReturn(List.of(
			item(UUID.randomUUID(), unscheduledDayId, "KTO", "999", 4)
		));

		handler.handle(command(place("126508", "성산일출봉"), place("126509", "우도")));

		ArgumentCaptor<ItineraryItemCreate> captor = ArgumentCaptor.forClass(ItineraryItemCreate.class);
		verify(commandRepository, times(2)).insertItem(captor.capture());
		assertThat(captor.getAllValues().get(0).sortOrder()).isEqualTo(5);
		assertThat(captor.getAllValues().get(1).sortOrder()).isEqualTo(6);
		assertThat(captor.getAllValues().get(0).itemType()).isEqualTo(ItineraryItemType.PLACE);
		assertThat(captor.getAllValues().get(0).itineraryDayId()).isEqualTo(unscheduledDayId);
	}

	@Test
	@DisplayName("같은 요청 안에 중복된 장소가 있으면 한 번만 추가한다")
	void deduplicatesWithinSingleRequest() {
		AddPlacesToUnscheduledResult result = handler.handle(command(
			place("126508", "성산일출봉"),
			place("126508", "성산일출봉")
		));

		assertThat(result.added()).hasSize(1);
		verify(commandRepository, times(1)).insertItem(any(ItineraryItemCreate.class));
	}

	@Test
	@DisplayName("빈 목록이면 아무것도 저장하지 않고 현재 version을 그대로 반환한다")
	void emptyRequestIsNoOp() {
		AddPlacesToUnscheduledResult result = handler.handle(new AddPlacesToUnscheduledCommand(
			tripId, actorId, List.of(), "vote-session"
		));

		assertThat(result.added()).isEmpty();
		assertThat(result.itineraryVersion()).isEqualTo(7L);
		verify(commandRepository, never()).insertItem(any(ItineraryItemCreate.class));
		verify(commandRepository, never()).insertDay(any(ItineraryDayCreate.class));
	}

	@Test
	@DisplayName("여행방 멤버가 아니면 추가할 수 없다")
	void nonMemberCannotAddPlaces() {
		when(tripAccessGuard.requireActiveMember(tripId, actorId))
			.thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "Trip member access is required."));

		assertThatThrownBy(() -> handler.handle(command(place("126508", "성산일출봉"))))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.FORBIDDEN));

		verify(commandRepository, never()).insertItem(any(ItineraryItemCreate.class));
	}

	@Test
	@DisplayName("추가 성공 시 협업 command event를 저장한다")
	void savesCollaborationEventPerAddedItem() {
		handler.handle(command(place("126508", "성산일출봉"), place("126509", "우도")));

		verify(eventRepository, times(2)).save(any());
	}

	private AddPlacesToUnscheduledCommand command(UnscheduledPlaceToAdd... places) {
		return new AddPlacesToUnscheduledCommand(tripId, actorId, List.of(places), "vote-session");
	}

	private UnscheduledPlaceToAdd place(String externalPlaceId, String name) {
		return new UnscheduledPlaceToAdd(
			"KTO", externalPlaceId, name, "제주특별자치도", 33.45, 126.94, null
		);
	}

	private ItineraryDayReadModel unscheduledDay() {
		return new ItineraryDayReadModel(
			unscheduledDayId, tripId, ItineraryDayGroupType.UNSCHEDULED, null, null, "일차 미정", 99
		);
	}

	private ItineraryItemReadModel item(UUID id, UUID dayId, String provider, String externalPlaceId) {
		return item(id, dayId, provider, externalPlaceId, 0);
	}

	private ItineraryItemReadModel item(UUID id, UUID dayId, String provider, String externalPlaceId, int sortOrder) {
		return new ItineraryItemReadModel(
			id, dayId, sortOrder, ItineraryItemType.PLACE, provider, externalPlaceId,
			"기존 장소", null, null, null, null, "AVAILABLE"
		);
	}
}
