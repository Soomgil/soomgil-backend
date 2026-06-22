package com.soomgil.record.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.ObjectStorageGateway;
import com.soomgil.global.storage.PresignedStorageRead;
import com.soomgil.global.storage.StorageReadRequest;
import com.soomgil.record.api.dto.CreateTripRecordRequest;
import com.soomgil.record.api.dto.PagedTripRecordEntry;
import com.soomgil.record.api.dto.PagedTripRecordPhoto;
import com.soomgil.record.api.dto.TripRecordPhotoSummaryResponse;
import com.soomgil.record.api.dto.UpdateTripRecordRequest;
import com.soomgil.record.application.port.ItineraryReferenceRepository;
import com.soomgil.record.application.port.RecordMediaAccessRepository;
import com.soomgil.record.application.port.TripRecordCommandRepository;
import com.soomgil.record.application.port.TripRecordCreateRequestReadModel;
import com.soomgil.record.application.port.TripRecordEntryCreate;
import com.soomgil.record.application.port.TripRecordEntryReadModel;
import com.soomgil.record.application.port.TripRecordEntryUpdate;
import com.soomgil.record.application.port.TripRecordMediaReadModel;
import com.soomgil.record.application.port.TripRecordPage;
import com.soomgil.record.application.port.TripRecordPhotoPage;
import com.soomgil.record.application.port.TripRecordPhotoReadModel;
import com.soomgil.record.application.port.TripRecordPhotoSummaryReadModel;
import com.soomgil.record.application.port.TripRecordPhotoUrlReadModel;
import com.soomgil.record.application.port.TripRecordQueryRepository;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TripRecordServiceTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID MEDIA_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	private final TripRecordCommandRepository commandRepository = mock(TripRecordCommandRepository.class);
	private final TripRecordQueryRepository queryRepository = mock(TripRecordQueryRepository.class);
	private final ItineraryReferenceRepository itineraryReferenceRepository = mock(ItineraryReferenceRepository.class);
	private final RecordMediaAccessRepository mediaAccessRepository = mock(RecordMediaAccessRepository.class);
	private final ObjectStorageGateway objectStorageGateway = mock(ObjectStorageGateway.class);
	private final TripRecordService service = new TripRecordService(
		commandRepository,
		queryRepository,
		itineraryReferenceRepository,
		mediaAccessRepository,
		objectStorageGateway,
		new TripAccessGuard(tripRepository()),
		() -> Instant.parse("2026-06-18T00:00:00Z")
	);

	@Test
	void createsRecordAndLinksMedia() {
		when(queryRepository.findEntry(any(), any())).thenReturn(Optional.of(entry(USER_ID)));
		when(queryRepository.findMedia(any())).thenReturn(List.of(media()));
		when(mediaAccessRepository.areLinkable(any(), org.mockito.ArgumentMatchers.eq(USER_ID), any())).thenReturn(true);

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
	void rejectsMediaNotOwnedByCurrentUser() {
		assertThatThrownBy(() -> service.createRecord(TRIP_ID, USER_ID, new CreateTripRecordRequest(
			null,
			null,
			"제목",
			null,
			null,
			null,
			null,
			null,
			List.of(MEDIA_ID)
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
		);
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
	void listsPhotosAcrossActiveTripsForCurrentUser() {
		when(queryRepository.findPhotosByUser(USER_ID, 0, 20))
			.thenReturn(new TripRecordPhotoPage(List.of(photo()), 1));

		PagedTripRecordPhoto result = service.listPhotos(USER_ID, 0, 20, List.of("createdAt,desc"));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().tripId()).isEqualTo(TRIP_ID);
		assertThat(result.page().totalElements()).isEqualTo(1);
	}

	@Test
	void returnsTheExistingRecordForTheSameIdempotencyRequest() {
		when(queryRepository.findEntry(any(), any())).thenReturn(Optional.of(entry(USER_ID)));
		when(queryRepository.findMedia(any())).thenReturn(List.of(media()));
		when(mediaAccessRepository.areLinkable(any(), org.mockito.ArgumentMatchers.eq(USER_ID), any())).thenReturn(true);
		CreateTripRecordRequest request = new CreateTripRecordRequest(
			null, null, "제목", null, null, null, null, null, List.of(MEDIA_ID)
		);

		service.createRecord(TRIP_ID, USER_ID, request, "record-request-1");
		ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
		verify(commandRepository).insertCreateRequest(
			org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.eq(TRIP_ID),
			org.mockito.ArgumentMatchers.eq("record-request-1"), hash.capture(), any(), any()
		);
		when(commandRepository.findCreateRequest(USER_ID, TRIP_ID, "record-request-1"))
			.thenReturn(new TripRecordCreateRequestReadModel(hash.getValue(), RECORD_ID));

		service.createRecord(TRIP_ID, USER_ID, request, "record-request-1");

		verify(commandRepository, times(1)).insertEntry(any(TripRecordEntryCreate.class));
		verify(commandRepository, times(2)).lockCreateRequest(USER_ID, TRIP_ID, "record-request-1");
	}

	@Test
	void createsThirtyMinuteServingUrlForPrivateRecordPhoto() {
		OffsetDateTime expiresAt = OffsetDateTime.parse("2026-06-18T00:30:00Z");
		when(queryRepository.findPhotosByUser(USER_ID, 0, 20))
			.thenReturn(new TripRecordPhotoPage(List.of(privatePhoto()), 1));
		when(objectStorageGateway.presignRead(any(StorageReadRequest.class))).thenReturn(new PresignedStorageRead(
			URI.create("https://storage.example.com/private-photo?X-Amz-Signature=signed"),
			expiresAt
		));

		PagedTripRecordPhoto result = service.listPhotos(USER_ID, 0, 20, List.of());

		assertThat(result.items().getFirst().media().publicUrl()).isNull();
		assertThat(result.items().getFirst().media().servingUrl())
			.hasToString("https://storage.example.com/private-photo?X-Amz-Signature=signed");
		assertThat(result.items().getFirst().media().servingUrlExpiresAt()).isEqualTo(expiresAt);
		ArgumentCaptor<StorageReadRequest> request = ArgumentCaptor.forClass(StorageReadRequest.class);
		verify(objectStorageGateway).presignRead(request.capture());
		assertThat(request.getValue().objectKey().value()).isEqualTo("media/user/trip-record/a.jpg");
		assertThat(request.getValue().validity()).isEqualTo(Duration.ofMinutes(30));
	}

	@Test
	void summarizesPhotosForTripsInOneRepositoryCall() {
		when(objectStorageGateway.presignRead(any(StorageReadRequest.class))).thenReturn(new PresignedStorageRead(
			URI.create("https://storage.example.com/cover?X-Amz-Signature=signed"),
			OffsetDateTime.parse("2026-06-18T00:30:00Z")
		));
		when(queryRepository.findPhotoSummariesByUser(USER_ID, List.of(TRIP_ID, OTHER_TRIP_ID)))
			.thenReturn(List.of(
				new TripRecordPhotoSummaryReadModel(OTHER_TRIP_ID, 0, null, null, null),
				new TripRecordPhotoSummaryReadModel(TRIP_ID, 3, MEDIA_ID, "media/user/trip-record/cover.jpg", null)
			));

		TripRecordPhotoSummaryResponse result = service.summarizePhotos(
			USER_ID,
			List.of(TRIP_ID, OTHER_TRIP_ID, TRIP_ID)
		);

		assertThat(result.items()).extracting(item -> item.tripId()).containsExactly(TRIP_ID, OTHER_TRIP_ID);
		assertThat(result.items().getFirst().photoCount()).isEqualTo(3);
		assertThat(result.items().getFirst().coverUrl())
			.isEqualTo(URI.create("https://storage.example.com/cover?X-Amz-Signature=signed"));
		assertThat(result.items().getFirst().coverMediaFileId()).isEqualTo(MEDIA_ID);
		assertThat(result.items().getFirst().coverUrlExpiresAt())
			.isEqualTo(OffsetDateTime.parse("2026-06-18T00:30:00Z"));
		verify(queryRepository).findPhotoSummariesByUser(USER_ID, List.of(TRIP_ID, OTHER_TRIP_ID));
	}

	@Test
	void rejectsPhotoSummaryWhenAnyRequestedTripIsNotAccessible() {
		when(queryRepository.findPhotoSummariesByUser(USER_ID, List.of(TRIP_ID, OTHER_TRIP_ID)))
			.thenReturn(List.of(new TripRecordPhotoSummaryReadModel(TRIP_ID, 0, null, null, null)));

		assertThatThrownBy(() -> service.summarizePhotos(USER_ID, List.of(TRIP_ID, OTHER_TRIP_ID)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	@Test
	void refreshesReadUrlOnlyForAnAccessibleRecordPhoto() {
		OffsetDateTime expiresAt = OffsetDateTime.parse("2026-06-18T00:30:00Z");
		when(queryRepository.findAccessiblePhotoUrl(USER_ID, MEDIA_ID)).thenReturn(Optional.of(
			new TripRecordPhotoUrlReadModel(MEDIA_ID, "media/user/trip-record/a.jpg", null)
		));
		when(objectStorageGateway.presignRead(any(StorageReadRequest.class))).thenReturn(new PresignedStorageRead(
			URI.create("https://storage.example.com/refreshed?X-Amz-Signature=signed"), expiresAt
		));

		var result = service.refreshPhotoReadUrl(USER_ID, MEDIA_ID);

		assertThat(result.mediaFileId()).isEqualTo(MEDIA_ID);
		assertThat(result.url()).hasToString("https://storage.example.com/refreshed?X-Amz-Signature=signed");
		assertThat(result.expiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void hidesAnInaccessibleRecordPhotoAsNotFound() {
		when(queryRepository.findAccessiblePhotoUrl(USER_ID, MEDIA_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.refreshPhotoReadUrl(USER_ID, MEDIA_ID))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
			);
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
	void preservesOmittedFieldsAndClearsExplicitNull() {
		when(queryRepository.findEntry(TRIP_ID, RECORD_ID)).thenReturn(Optional.of(entry(USER_ID)));
		when(queryRepository.findMedia(RECORD_ID)).thenReturn(List.of());
		UpdateTripRecordRequest request = new UpdateTripRecordRequest();
		request.setTitle(" 수정 제목 ");
		request.setCaption(null);

		service.updateRecord(TRIP_ID, USER_ID, RECORD_ID, request);

		ArgumentCaptor<TripRecordEntryUpdate> captor = ArgumentCaptor.forClass(TripRecordEntryUpdate.class);
		verify(commandRepository).updateEntry(captor.capture());
		TripRecordEntryUpdate update = captor.getValue();
		assertThat(update.title()).isEqualTo("수정 제목");
		assertThat(update.caption()).isNull();
		assertThat(update.locationName()).isEqualTo("서울");
		assertThat(update.lat()).isEqualTo(37.5);
		assertThat(update.lng()).isEqualTo(127.0);
		assertThat(update.takenAt()).isEqualTo(OffsetDateTime.parse("2026-06-18T10:00:00Z"));
	}

	@Test
	void rejectsItineraryDayFromAnotherTrip() {
		UUID dayId = UUID.fromString("50000000-0000-0000-0000-000000000001");

		assertThatThrownBy(() -> service.createRecord(TRIP_ID, USER_ID, new CreateTripRecordRequest(
			dayId,
			null,
			"제목",
			null,
			null,
			null,
			null,
			null,
			List.of()
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
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
			"media/user/trip-record/a.jpg",
			"https://example.com/a.jpg",
			"image/jpeg",
			100L,
			100,
			100,
			"ACTIVE",
			OffsetDateTime.parse("2026-06-18T00:00:00Z"),
			0
		);
	}

	private TripRecordPhotoReadModel photo() {
		return new TripRecordPhotoReadModel(
			TRIP_ID,
			"부산 여행",
			RECORD_ID,
			null,
			null,
			USER_ID,
			MEDIA_ID,
			"media/user/trip-record/a.jpg",
			"https://example.com/a.jpg",
			"image/jpeg",
			100L,
			100,
			100,
			"ACTIVE",
			OffsetDateTime.parse("2026-06-18T00:00:00Z"),
			OffsetDateTime.parse("2026-06-18T10:00:00Z"),
			OffsetDateTime.parse("2026-06-18T00:00:00Z")
		);
	}

	private TripRecordPhotoReadModel privatePhoto() {
		return new TripRecordPhotoReadModel(
			TRIP_ID,
			"부산 여행",
			RECORD_ID,
			null,
			null,
			USER_ID,
			MEDIA_ID,
			"media/user/trip-record/a.jpg",
			null,
			"image/jpeg",
			100L,
			100,
			100,
			"ACTIVE",
			OffsetDateTime.parse("2026-06-18T00:00:00Z"),
			OffsetDateTime.parse("2026-06-18T10:00:00Z"),
			OffsetDateTime.parse("2026-06-18T00:00:00Z")
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
