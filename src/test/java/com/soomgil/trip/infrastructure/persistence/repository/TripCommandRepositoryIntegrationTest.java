package com.soomgil.trip.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.domain.model.TripMember;
import java.time.Instant;
import java.time.OffsetDateTime;
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
class TripCommandRepositoryIntegrationTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000011");
	private static final UUID OWNER_ID = UUID.fromString("20000000-0000-0000-0000-000000000011");
	private static final UUID MEMBER_ID = UUID.fromString("20000000-0000-0000-0000-000000000012");
	private static final UUID INVITE_ID = UUID.fromString("30000000-0000-0000-0000-000000000011");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TripCommandRepository repository;

	@Test
	void onlyFirstPendingInviteClaimSucceeds() {
		insertTrip();
		OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
		jdbcTemplate.update(
			"INSERT INTO trip.trip_invites "
				+ "(id, trip_id, created_by_user_id, invite_code, invite_token_hash, status, created_at) "
				+ "VALUES (?, ?, ?, 'ABCD1234', 'unique-hash', 'PENDING', ?)",
			INVITE_ID,
			TRIP_ID,
			OWNER_ID,
			now
		);

		assertThat(repository.acceptTripInvite(INVITE_ID, MEMBER_ID, now.toInstant())).isTrue();
		assertThat(repository.acceptTripInvite(INVITE_ID, UUID.randomUUID(), now.toInstant())).isFalse();
	}

	@Test
	void reactivatesExistingRemovedMember() {
		insertTrip();
		UUID existingMembershipId = UUID.fromString("40000000-0000-0000-0000-000000000011");
		OffsetDateTime oldJoinedAt = OffsetDateTime.parse("2026-06-01T00:00:00Z");
		jdbcTemplate.update(
			"INSERT INTO trip.trip_members "
				+ "(id, trip_id, user_id, role, status, joined_at, left_at, removed_by_user_id) "
				+ "VALUES (?, ?, ?, 'MEMBER', 'REMOVED', ?, ?, ?)",
			existingMembershipId,
			TRIP_ID,
			MEMBER_ID,
			oldJoinedAt,
			oldJoinedAt,
			OWNER_ID
		);
		Instant rejoinedAt = Instant.parse("2026-06-18T00:00:00Z");

		repository.addTripMember(TripMember.activeMember(UUID.randomUUID(), TRIP_ID, MEMBER_ID, rejoinedAt));

		assertThat(jdbcTemplate.queryForObject(
			"SELECT status FROM trip.trip_members WHERE trip_id = ? AND user_id = ?",
			String.class,
			TRIP_ID,
			MEMBER_ID
		)).isEqualTo("ACTIVE");
		assertThat(jdbcTemplate.queryForObject(
			"SELECT count(*) FROM trip.trip_members WHERE trip_id = ? AND user_id = ?",
			Long.class,
			TRIP_ID,
			MEMBER_ID
		)).isEqualTo(1L);
	}

	private void insertTrip() {
		OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
		jdbcTemplate.update(
			"INSERT INTO trip.trips (id, owner_user_id, title, status, itinerary_version, created_at, updated_at) "
				+ "VALUES (?, ?, 'test', 'ACTIVE', 0, ?, ?)",
			TRIP_ID,
			OWNER_ID,
			now,
			now
		);
	}
}
