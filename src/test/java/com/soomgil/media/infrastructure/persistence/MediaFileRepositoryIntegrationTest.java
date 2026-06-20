package com.soomgil.media.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.application.port.LinkedMediaResourceAuthorizer;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.domain.model.MediaFileMetadata;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import com.soomgil.global.storage.ObjectStorageGateway;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class MediaFileRepositoryIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID MEDIA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID TRIP_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID RECORD_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-20T12:00:00Z");

	@MockitoBean
	private ObjectStorageGateway objectStorageGateway;

	@Autowired
	private MediaFileRepository repository;

	@Autowired
	private LinkedMediaResourceAuthorizer authorizer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void savesReadsAndSoftDeletesMediaMetadata() {
		MediaFileMetadata mediaFile = mediaFile();

		repository.save(mediaFile);
		assertThat(repository.findById(MEDIA_ID)).isEqualTo(mediaFile);

		Instant deletedAt = Instant.parse("2026-06-21T00:00:00Z");
		Instant purgeAfter = Instant.parse("2026-06-28T00:00:00Z");
		assertThat(repository.markDeleted(MEDIA_ID, deletedAt, purgeAfter)).isTrue();

		MediaFileMetadata deleted = repository.findById(MEDIA_ID);
		assertThat(deleted.status()).isEqualTo("DELETED");
		assertThat(deleted.deletedAt()).isEqualTo(OffsetDateTime.parse("2026-06-21T00:00:00Z"));
		assertThat(deleted.purgeAfterAt()).isEqualTo(OffsetDateTime.parse("2026-06-28T00:00:00Z"));
	}

	@Test
	void authorizesSelfProfileAndActiveTripRecordMemberOnly() {
		insertTripRecord();

		assertThat(authorizer.canLink(USER_ID, "USER_PROFILE", USER_ID)).isTrue();
		assertThat(authorizer.canLink(USER_ID, "USER_PROFILE", UUID.randomUUID())).isFalse();
		assertThat(authorizer.canLink(USER_ID, "TRIP_RECORD", RECORD_ID)).isTrue();
		assertThat(authorizer.canLink(UUID.randomUUID(), "TRIP_RECORD", RECORD_ID)).isFalse();
		assertThat(authorizer.canLink(USER_ID, "UNKNOWN", RECORD_ID)).isFalse();
	}

	private MediaFileMetadata mediaFile() {
		return new MediaFileMetadata(
			MEDIA_ID, USER_ID, "S3_COMPATIBLE", "soomgil-media",
			new StorageObjectKey("media/" + USER_ID + "/profile-image/file.jpg"),
			URI.create("https://cdn.example.com/file.jpg"), "image/jpeg", 1024L,
			100, 200, null, null, "ACTIVE", NOW, null, null
		);
	}

	private void insertTripRecord() {
		jdbcTemplate.update(
			"INSERT INTO trip.trips (id, owner_user_id, title, status, itinerary_version, created_at, updated_at) "
				+ "VALUES (?, ?, 'media test', 'ACTIVE', 0, ?, ?)",
			TRIP_ID, USER_ID, NOW, NOW
		);
		jdbcTemplate.update(
			"INSERT INTO trip.trip_members (id, trip_id, user_id, role, status, joined_at) "
				+ "VALUES (?, ?, ?, 'MEMBER', 'ACTIVE', ?)",
			UUID.randomUUID(), TRIP_ID, USER_ID, NOW
		);
		jdbcTemplate.update(
			"INSERT INTO record.trip_record_entries "
				+ "(id, trip_id, uploaded_by_user_id, visibility, status, created_at, updated_at) "
				+ "VALUES (?, ?, ?, 'TRIP_MEMBERS', 'ACTIVE', ?, ?)",
			RECORD_ID, TRIP_ID, USER_ID, NOW, NOW
		);
	}
}
