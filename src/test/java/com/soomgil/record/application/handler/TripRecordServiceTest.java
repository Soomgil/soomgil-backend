package com.soomgil.record.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.record.api.dto.CreateTripRecordRequest;
import com.soomgil.record.api.dto.PagedTripRecordEntry;
import com.soomgil.record.api.dto.UpdateTripRecordRequest;
import com.soomgil.record.application.port.TripRecordCommandRepository;
import com.soomgil.record.application.port.TripRecordEntryCreate;
import com.soomgil.record.application.port.TripRecordEntryReadModel;
import com.soomgil.record.application.port.TripRecordMediaReadModel;
import com.soomgil.record.application.port.TripRecordPage;
import com.soomgil.record.application.port.TripRecordQueryRepository;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripRecordServiceTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID MEDIA_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	private final TripRecordCommandRepository commandRepository = mock(TripRecordCommandRepository.class);
	private final TripRecordQueryRepository queryRepository = mock(TripRecordQueryRepository.class);
	private final TripRecordService service = new TripRecordService(
		commandRepository,
		queryRepository,
		new TripAccessGuard(tripRepository()),
		() -> Instant.parse("2026-06-18T00:00:00Z")
	);

	@Test
	void createsRecordAndLinksMedia() {
		when(queryRepository.findEntry(any(), any())).thenReturn(Optional.of(entry(USER_ID)));
		when(queryRepository.findMedia(any())).thenReturn(List.of(media()));

		var result = service.createRecord(TRIP_ID, USER_ID, new CreateTripRecordRequest(
			null,
			null,
			" 제목 ",
			"본문",
			"서울",
			37.5,
			127.0,
			OffsetDateTime.parse("2026-06-18T10:00:00Z"),
			List.of(MEDIA_ID)
		));

		assertThat(result.tripId()).isEqualTo(TRIP_ID);
		assertThat(result.media()).hasSize(1);
		verify(commandRepository).insertEntry(any(TripRecordEntryCreate.class));
		verify(commandRepository).insertMediaLinks(any(), org.mockito.ArgumentMatchers.eq(List.of(MEDIA_ID)), any());
	}

	@Test
	void listsRecordsForTripMember() {
		when(queryRepository.findEntries(TRIP_ID, 0, 20)).thenReturn(new TripRecordPage(List.of(entry(USER_ID)), 1));
		when(queryRepository.findMedia(RECORD_ID)).thenReturn(List.of());

		PagedTripRecordEntry result = service.listRecords(TRIP_ID, USER_ID, 0, 20, List.of("createdAt,desc"));

		assertThat(result.items()).hasSize(1);
		assertThat(result.page().totalElements()).isEqualTo(1);
	}

	@Test
	void rejectsUpdateByNonUploader() {
		when(queryRepository.findEntry(TRIP_ID, RECORD_ID)).thenReturn(Optional.of(entry(OTHER_USER_ID)));

		assertThatThrownBy(() -> service.updateRecord(TRIP_ID, USER_ID, RECORD_ID, new UpdateTripRecordRequest(
			null,
			null,
			"수정",
			null,
			null,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
		);
	}

	@Test
	void deletesRecordAndMediaLinks() {
		when(queryRepository.findEntry(TRIP_ID, RECORD_ID)).thenReturn(Optional.of(entry(USER_ID)));
		when(commandRepository.softDeleteEntry(any(), any(), any())).thenReturn(true);

		service.deleteRecord(TRIP_ID, USER_ID, RECORD_ID);

		verify(commandRepository).softDeleteEntry(org.mockito.ArgumentMatchers.eq(TRIP_ID), org.mockito.ArgumentMatchers.eq(RECORD_ID), any());
		verify(commandRepository).deleteMediaLinks(RECORD_ID);
	}

	private TripRecordEntryReadModel entry(UUID uploadedByUserId) {
		return new TripRecordEntryReadModel(
			RECORD_ID,
			TRIP_ID,
			"부산 여행",
			null,
			null,
			uploadedByUserId,
			"제목",
			"본문",
			"서울",
			37.5,
			127.0,
			OffsetDateTime.parse("2026-06-18T10:00:00Z"),
			"TRIP_MEMBERS",
			"ACTIVE",
			OffsetDateTime.parse("2026-06-18T00:00:00Z")
		);
	}

	private TripRecordMediaReadModel media() {
		return new TripRecordMediaReadModel(
			RECORD_ID,
			MEDIA_ID,
			URI.create("https://example.com/a.jpg"),
			"image/jpeg",
			100L,
			100,
			100,
			"ACTIVE",
			OffsetDateTime.parse("2026-06-18T00:00:00Z"),
			0
		);
	}

	private TripQueryRepository tripRepository() {
		TripQueryRepository repository = mock(TripQueryRepository.class);
		when(repository.findTripAccess(TRIP_ID, USER_ID)).thenReturn(Optional.of(new TripAccessSnapshot(
			TRIP_ID,
			USER_ID,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			USER_ID
		)));
		return repository;
	}
}
