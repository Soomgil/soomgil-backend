package com.soomgil.preference.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import({
	TestcontainersConfiguration.class,
	PlaceRecommendationQueryHandlerIntegrationTest.CurrentUserTestConfiguration.class
})
@SpringBootTest
class PlaceRecommendationQueryHandlerIntegrationTest {

	private static final UUID TRIP_ID = UUID.fromString("00000000-0000-0000-0000-000000002101");
	private static final UUID MEMBER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000002102");
	private static final UUID MEMBER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000002103");
	private static final UUID CAFE_ENRICHMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000002104");
	private static final UUID BEACH_ENRICHMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000002105");

	@Autowired
	private ListPlaceRecommendationsQueryHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		createUserProfileContract();
		cleanUp();
		insertTripMembers();
		insertUserProfiles();
		insertPlaces();
		insertPlaceTags();
		insertMemberPreferences();
	}

	@Test
	void ranksViewportPlacesUsingPersistedMemberPreferences() {
		var result = handler.handle(new ListPlaceRecommendationsQuery(
			TRIP_ID,
			"129.0,35.0,130.0,36.0",
			35.5,
			129.5,
			RecommendationTab.BASIC,
			0,
			20
		));

		assertThat(result.items()).extracting(item -> item.place().externalPlaceId())
			.containsExactly("1001", "1002");
		assertThat(result.items().getFirst().matchedMembers())
			.extracting(member -> member.displayName())
			.containsExactly("민경철", "여행 친구");
		assertThat(result.items().get(1).matchedMembers()).isEmpty();
	}

	private void createUserProfileContract() {
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS auth");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS auth.user_profiles (
				user_id uuid PRIMARY KEY,
				display_name varchar(80) NOT NULL,
				profile_image_url text
			)
			""");
	}

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM preference.user_preference_tag_weights");
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_tags");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_candidates");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichments");
		jdbcTemplate.update("DELETE FROM trip.trip_members");
		jdbcTemplate.update("DELETE FROM trip.trips");
		jdbcTemplate.update("DELETE FROM auth.user_profiles");
		jdbcTemplate.update("DELETE FROM tourism_source.attraction_images");
		jdbcTemplate.update("DELETE FROM tourism_source.attractions");
		jdbcTemplate.update("DELETE FROM tourism_source.contenttypes");
	}

	private void insertTripMembers() {
		jdbcTemplate.update("""
			INSERT INTO trip.trips (
				id, owner_user_id, title, status, created_at, updated_at
			)
			VALUES (?, ?, 'Busan trip', 'ACTIVE', now(), now())
			""", TRIP_ID, MEMBER_A_ID);
		jdbcTemplate.update("""
			INSERT INTO trip.trip_members (
				id, trip_id, user_id, role, status, joined_at
			)
			VALUES
				('00000000-0000-0000-0000-000000002106', ?, ?, 'MEMBER', 'ACTIVE', now()),
				('00000000-0000-0000-0000-000000002107', ?, ?, 'MEMBER', 'ACTIVE', now())
			""", TRIP_ID, MEMBER_A_ID, TRIP_ID, MEMBER_B_ID);
	}

	private void insertUserProfiles() {
		jdbcTemplate.update("""
			INSERT INTO auth.user_profiles (user_id, display_name)
			VALUES (?, '민경철'), (?, '여행 친구')
			""", MEMBER_A_ID, MEMBER_B_ID);
	}

	private void insertPlaces() {
		jdbcTemplate.update("""
			INSERT INTO tourism_source.contenttypes (content_type_id, content_type_name)
			VALUES (12, 'ATTRACTION')
			""");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.attractions (
				no, content_id, title, content_type_id, latitude, longitude, addr1,
				source_modified_at, imported_at
			)
			VALUES
				(101, 1001, 'Quiet Cafe', 12, 35.5, 129.5, 'Busan', now(), now()),
				(102, 1002, 'Blue Beach', 12, 35.6, 129.6, 'Busan', now(), now())
			""");
	}

	private void insertPlaceTags() {
		UUID quietTagId = tagId("quiet");
		UUID scenicTagId = tagId("scenic_view");
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichments (
				id, provider, external_place_id, status, selected_count, enriched_at
			)
			VALUES
				(?, 'KTO', '1001', 'SUCCEEDED', 2, now()),
				(?, 'KTO', '1002', 'SUCCEEDED', 2, now())
			""", CAFE_ENRICHMENT_ID, BEACH_ENRICHMENT_ID);
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichment_tags (
				enrichment_id, tag_id, confidence, weight, rank_order
			)
			VALUES
				(?, ?, 1.0, 0.75, 1),
				(?, ?, 1.0, 0.25, 2),
				(?, ?, 1.0, 0.25, 1),
				(?, ?, 1.0, 0.75, 2)
			""",
			CAFE_ENRICHMENT_ID, quietTagId,
			CAFE_ENRICHMENT_ID, scenicTagId,
			BEACH_ENRICHMENT_ID, quietTagId,
			BEACH_ENRICHMENT_ID, scenicTagId
		);
	}

	private void insertMemberPreferences() {
		UUID quietTagId = tagId("quiet");
		UUID scenicTagId = tagId("scenic_view");
		jdbcTemplate.update("""
			INSERT INTO preference.user_preference_tag_weights (
				user_id, tag_id, preference_score
			)
			VALUES
				(?, ?, 0.90),
				(?, ?, 0.50),
				(?, ?, 0.70),
				(?, ?, 0.60)
			""",
			MEMBER_A_ID, quietTagId,
			MEMBER_A_ID, scenicTagId,
			MEMBER_B_ID, quietTagId,
			MEMBER_B_ID, scenicTagId
		);
	}

	private UUID tagId(String code) {
		return jdbcTemplate.queryForObject(
			"SELECT id FROM preference.preference_tags WHERE code = ?",
			UUID.class,
			code
		);
	}

	@TestConfiguration
	static class CurrentUserTestConfiguration {

		@Bean
		CurrentUserProvider currentUserProvider() {
			return () -> new CurrentUser(MEMBER_A_ID, "min@example.com");
		}
	}
}
