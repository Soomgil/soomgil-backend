package com.soomgil.record.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.record.application.port.RecordMediaAccessRepository;
import com.soomgil.record.application.port.TripRecordCommandRepository;
import com.soomgil.record.application.port.TripRecordQueryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RecordMediaAccessRepositoryIntegrationTest {

	private static final UUID OWNER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID EMPTY_TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final UUID FIRST_RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID SECOND_RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
	private static final UUID MEDIA_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID SECOND_MEDIA_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RecordMediaAccessRepository repository;

	@Autowired
	private TripRecordQueryRepository queryRepository;

	@Autowired
	private TripRecordCommandRepository commandRepository;

	@Test
	void allowsCurrentLinkButRejectsLinkToAnotherRecord() {
		insertFixtures();

		assertThat(repository.areLinkable(FIRST_RECORD_ID, OWNER_ID, List.of(MEDIA_ID))).isTrue();
		assertThat(repository.areLinkable(SECOND_RECORD_ID, OWNER_ID, List.of(MEDIA_ID))).isFalse();
	}

	@Test
	void summarizesPhotoCountsAndCoverForRequestedTripsInOneQuery() {
		insertFixtures();
		insertEmptyTrip();

		var summaries = queryRepository.findPhotoSummariesByUser(OWNER_ID, List.of(TRIP_ID, EMPTY_TRIP_ID));

		assertThat(summaries).hasSize(2);
		assertThat(summaries).filteredOn(summary -> summary.tripId().equals(TRIP_ID)).singleElement()
			.satisfies(summary -> {
				assertThat(summary.photoCount()).isEqualTo(1);
				assertThat(summary.coverMediaFileId()).isEqualTo(MEDIA_ID);
				assertThat(summary.coverObjectKey()).isEqualTo("record/photo.jpg");
				assertThat(summary.coverPublicUrl()).isEqualTo("https://example.com/record/photo.jpg");
			});
		assertThat(summaries).filteredOn(summary -> summary.tripId().equals(EMPTY_TRIP_ID)).singleElement()
			.satisfies(summary -> {
				assertThat(summary.photoCount()).isZero();
				assertThat(summary.coverObjectKey()).isNull();
				assertThat(summary.coverPublicUrl()).isNull();
			});
	}

	@Test
	void selectsCoverKeyAndPublicUrlFromTheSamePhoto() {
		insertFixtures();
		insertSecondPhoto();

		var summary = queryRepository.findPhotoSummariesByUser(OWNER_ID, List.of(TRIP_ID)).getFirst();

		assertThat(summary.photoCount()).isEqualTo(2);
		assertThat(summary.coverMediaFileId()).isEqualTo(SECOND_MEDIA_ID);
		assertThat(summary.coverObjectKey()).isEqualTo("record/photo-2.jpg");
		assertThat(summary.coverPublicUrl()).isNull();
	}

	@Test
	void findsPhotoUrlOnlyThroughAnAccessibleActiveRecord() {
		insertFixtures();

		assertThat(queryRepository.findAccessiblePhotoUrl(OWNER_ID, MEDIA_ID))
			.hasValueSatisfying(photo -> {
				assertThat(photo.mediaFileId()).isEqualTo(MEDIA_ID);
				assertThat(photo.objectKey()).isEqualTo("record/photo.jpg");
			});
		assertThat(queryRepository.findAccessiblePhotoUrl(UUID.randomUUID(), MEDIA_ID)).isEmpty();
	}

	@Test
	void pagesPhotosInAStableOrderWhenRecordTimestampsAreEqual() {
		insertFixtures();
		insertSecondPhoto();
		jdbcTemplate.update("UPDATE media.media_files SET public_url = NULL WHERE id = ?", MEDIA_ID);

		var firstPage = queryRepository.findPhotos(TRIP_ID, 0, 1);
		var secondPage = queryRepository.findPhotos(TRIP_ID, 1, 1);

		assertThat(firstPage.items()).extracting(item -> item.mediaFileId()).containsExactly(SECOND_MEDIA_ID);
		assertThat(secondPage.items()).extracting(item -> item.mediaFileId()).containsExactly(MEDIA_ID);
	}

	@Test
	void storesAndReadsRecordCreateIdempotencyRequest() {
		insertFixtures();
		OffsetDateTime now = OffsetDateTime.parse("2026-06-22T00:00:00Z");

		commandRepository.lockCreateRequest(OWNER_ID, TRIP_ID, "record-create-key");
		commandRepository.insertCreateRequest(
			OWNER_ID, TRIP_ID, "record-create-key", "request-hash", FIRST_RECORD_ID, now
		);

		assertThat(commandRepository.findCreateRequest(OWNER_ID, TRIP_ID, "record-create-key"))
			.satisfies(request -> {
				assertThat(request.requestHash()).isEqualTo("request-hash");
				assertThat(request.recordId()).isEqualTo(FIRST_RECORD_ID);
			});
	}

	private void insertFixtures() {
		OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
		jdbcTemplate.update(
			"INSERT INTO trip.trips (id, owner_user_id, title, status, itinerary_version, created_at, updated_at) "
				+ "VALUES (?, ?, 'test', 'ACTIVE', 0, ?, ?)",
			TRIP_ID,
			OWNER_ID,
			now,
			now
		);
		jdbcTemplate.update(
			"INSERT INTO record.trip_record_entries "
				+ "(id, trip_id, uploaded_by_user_id, visibility, status, created_at, updated_at) "
				+ "VALUES (?, ?, ?, 'TRIP_MEMBERS', 'ACTIVE', ?, ?), (?, ?, ?, 'TRIP_MEMBERS', 'ACTIVE', ?, ?)",
			FIRST_RECORD_ID,
			TRIP_ID,
			OWNER_ID,
			now,
			now,
			SECOND_RECORD_ID,
			TRIP_ID,
			OWNER_ID,
			now,
			now
		);
		jdbcTemplate.update(
			"INSERT INTO media.media_files "
				+ "(id, owner_user_id, bucket, object_key, public_url, mime_type, status, created_at) "
				+ "VALUES (?, ?, 'test', 'record/photo.jpg', 'https://example.com/record/photo.jpg', 'image/jpeg', 'ACTIVE', ?)",
			MEDIA_ID,
			OWNER_ID,
			now
		);
		jdbcTemplate.update(
			"INSERT INTO record.trip_record_media (record_entry_id, media_file_id, sort_order, created_at) "
				+ "VALUES (?, ?, 0, ?)",
			FIRST_RECORD_ID,
			MEDIA_ID,
			now
		);
	}

	private void insertEmptyTrip() {
		OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
		jdbcTemplate.update(
			"INSERT INTO trip.trips (id, owner_user_id, title, status, itinerary_version, created_at, updated_at) "
				+ "VALUES (?, ?, 'empty', 'ARCHIVED', 0, ?, ?)",
			EMPTY_TRIP_ID,
			OWNER_ID,
			now,
			now
		);
	}

	private void insertSecondPhoto() {
		OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
		jdbcTemplate.update(
			"INSERT INTO media.media_files "
				+ "(id, owner_user_id, bucket, object_key, mime_type, status, created_at) "
				+ "VALUES (?, ?, 'test', 'record/photo-2.jpg', 'image/jpeg', 'ACTIVE', ?)",
			SECOND_MEDIA_ID,
			OWNER_ID,
			now
		);
		jdbcTemplate.update(
			"INSERT INTO record.trip_record_media (record_entry_id, media_file_id, sort_order, created_at) "
				+ "VALUES (?, ?, 0, ?)",
			SECOND_RECORD_ID,
			SECOND_MEDIA_ID,
			now
		);
	}
}
