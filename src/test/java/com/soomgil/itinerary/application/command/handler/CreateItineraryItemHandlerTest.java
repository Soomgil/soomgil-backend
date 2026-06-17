package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateItineraryItemHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CreateItineraryItemHandler handler = new CreateItineraryItemHandler(
		repository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z")
	);

	@Test
	void createsPlaceItem() {
		ItineraryMutationResult result = handler.handle(new CreateItineraryItemCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			2,
			ItineraryItemType.PLACE,
			"KTO",
			"126508",
			"성심당",
			"대전 중구",
			36.3275,
			127.4272,
			null
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.item().itineraryDayId()).isEqualTo(DAY_ID);
		assertThat(result.item().placeProvider()).isEqualTo("KTO");
		assertThat(repository.insertedItem.createdByUserId()).isEqualTo(USER_ID);
	}

	@Test
	void rejectsMissingDay() {
		repository.dayExists = false;

		assertThatThrownBy(() -> handler.handle(new CreateItineraryItemCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			0,
			ItineraryItemType.CUSTOM_PLACE,
			null,
			null,
			"임시 장소",
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
		);
	}

	@Test
	void rejectsPlaceItemWithoutPlaceReference() {
		assertThatThrownBy(() -> handler.handle(new CreateItineraryItemCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			0,
			ItineraryItemType.PLACE,
			null,
			null,
			"성심당",
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private boolean dayExists = true;
		private long currentVersion;
		private ItineraryItemCreate insertedItem;

		@Override
		public OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt) {
			if (currentVersion != baseVersion) {
				return OptionalLong.empty();
			}
			currentVersion++;
			return OptionalLong.of(currentVersion);
		}

		@Override
		public void insertDay(ItineraryDayCreate day) {
		}

		@Override
		public void insertItem(ItineraryItemCreate item) {
			this.insertedItem = item;
		}

		@Override
		public boolean existsDay(UUID tripId, UUID dayId) {
			return dayExists;
		}
	}
}
