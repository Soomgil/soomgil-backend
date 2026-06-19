package com.soomgil.record.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.record.application.port.RecordMediaAccessRepository;
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
	private static final UUID FIRST_RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID SECOND_RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
	private static final UUID MEDIA_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RecordMediaAccessRepository repository;

	@Test
	void allowsCurrentLinkButRejectsLinkToAnotherRecord() {
		insertFixtures();

		assertThat(repository.areLinkable(FIRST_RECORD_ID, OWNER_ID, List.of(MEDIA_ID))).isTrue();
		assertThat(repository.areLinkable(SECOND_RECORD_ID, OWNER_ID, List.of(MEDIA_ID))).isFalse();
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
			"INSERT INTO media.media_files (id, owner_user_id, bucket, object_key, status, created_at) "
				+ "VALUES (?, ?, 'test', 'record/photo.jpg', 'ACTIVE', ?)",
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
}
