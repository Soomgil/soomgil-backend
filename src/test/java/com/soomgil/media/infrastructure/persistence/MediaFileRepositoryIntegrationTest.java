package com.soomgil.media.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.application.port.LinkedMediaResourceAuthorizer;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.infrastructure.persistence.mapper.MediaFileMapper;
import com.soomgil.media.infrastructure.persistence.mapper.MediaUploadIntentMapper;
import com.soomgil.media.infrastructure.persistence.row.MediaUploadIntentRow;
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

	@Autowired
	private MediaFileMapper mediaFileMapper;

	@Autowired
	private MediaUploadIntentMapper uploadIntentMapper;

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
	void rejectsRemovedRecordLinksAndAuthorizesActiveTripMembers() {
		insertTrip();

		assertThat(authorizer.canLink(USER_ID, "USER_PROFILE", USER_ID)).isTrue();
		assertThat(authorizer.canLink(USER_ID, "USER_PROFILE", UUID.randomUUID())).isFalse();
		assertThat(authorizer.canLink(USER_ID, "TRIP_RECORD", RECORD_ID)).isFalse();
		assertThat(authorizer.canLink(UUID.randomUUID(), "TRIP_RECORD", RECORD_ID)).isFalse();
		assertThat(authorizer.canLink(USER_ID, "UNKNOWN", RECORD_ID)).isFalse();
		assertThat(authorizer.canLink(USER_ID, "TRIP", TRIP_ID)).isTrue();
	}

	@Test
	void keepsThreadAttachedMediaOutOfOrphanCleanup() {
		// 쓰레드에 붙은 미디어는 linked_resource가 비어 있어도 고아 정리 대상이 아니다.
		repository.save(mediaFile());
		jdbcTemplate.update("INSERT INTO auth.users (id) VALUES (?) ON CONFLICT DO NOTHING", USER_ID);
		UUID threadId = UUID.randomUUID();
		jdbcTemplate.update(
			"INSERT INTO community.threads (id, author_user_id, content, created_at, updated_at) "
				+ "VALUES (?, ?, '미디어 정리 보호 테스트', ?, ?)",
			threadId, USER_ID, NOW, NOW
		);
		jdbcTemplate.update(
			"INSERT INTO community.thread_media (thread_id, media_file_id, sort_order, created_at) "
				+ "VALUES (?, ?, 0, ?)",
			threadId, MEDIA_ID, NOW
		);
		jdbcTemplate.update(
			"INSERT INTO media.upload_intents (id, owner_user_id, object_key, status, media_file_id, expires_at, created_at, completed_at) "
				+ "VALUES (?, ?, ?, 'COMPLETED', ?, ?, ?, ?)",
			UUID.randomUUID(), USER_ID, "media/" + USER_ID + "/community-post/thread.jpg",
			MEDIA_ID, NOW.minusMinutes(5), NOW.minusHours(1), NOW.minusHours(1)
		);

		OffsetDateTime now = NOW.plusDays(1);
		assertThat(uploadIntentMapper.findExpiredCompletedUnlinked(now, 100))
			.extracting(MediaUploadIntentRow::mediaFileId)
			.doesNotContain(MEDIA_ID);
		assertThat(mediaFileMapper.claimUnlinkedForPurge(MEDIA_ID, now.toInstant())).isZero();
	}

	private MediaFileMetadata mediaFile() {
		return new MediaFileMetadata(
			MEDIA_ID, USER_ID, "S3_COMPATIBLE", "soomgil-media",
			new StorageObjectKey("media/" + USER_ID + "/profile-image/file.jpg"),
			URI.create("https://cdn.example.com/file.jpg"), "image/jpeg", 1024L,
			100, 200, null, null, "ACTIVE", NOW, null, null
		);
	}

	private void insertTrip() {
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

	}
}
