package com.soomgil.preference.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
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
	SwipeFeedQueryHandlerIntegrationTest.CurrentUserTestConfiguration.class
})
@SpringBootTest
class SwipeFeedQueryHandlerIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");

	@Autowired
	private SwipeFeedQueryHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		createExternalContractFixtures();
		jdbcTemplate.update("DELETE FROM preference.user_saved_places");
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
		jdbcTemplate.update("DELETE FROM social.user_follows");
		jdbcTemplate.update("DELETE FROM auth.user_profiles");
		jdbcTemplate.update("DELETE FROM tourism_source.attraction_images");
		jdbcTemplate.update("DELETE FROM tourism_source.attractions");
		jdbcTemplate.update("DELETE FROM tourism_source.contenttypes");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.contenttypes (content_type_id, content_type_name)
			VALUES (12, 'ATTRACTION')
			""");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.attractions (
				no,
				content_id,
				title,
				content_type_id,
				area_code,
				si_gun_gu_code,
				latitude,
				longitude,
				addr1,
				source_modified_at,
				imported_at
			)
			VALUES
				(1, 126508, 'Haeundae Beach', 12, 26, 2, 35.1587, 129.1604, 'Busan Haeundae-gu', '2026-06-02T00:00:00Z', '2026-06-02T00:00:00Z'),
				(2, 999999, 'Seoul Forest', 12, 11, 0, 37.5444, 127.0374, 'Seoul Seongdong-gu', '2026-06-01T00:00:00Z', '2026-06-01T00:00:00Z')
			""");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.attraction_images (
				id,
				attraction_no,
				source_type,
				public_url,
				display_order
			)
			VALUES
				('00000000-0000-0000-0000-000000000902', 1, 'THUMBNAIL', 'https://cdn.soomgil.example.com/places/126508.jpg', 1),
				('00000000-0000-0000-0000-000000000903', 2, 'THUMBNAIL', 'https://cdn.soomgil.example.com/places/999999.jpg', 1)
			""");
		jdbcTemplate.update("""
			INSERT INTO preference.user_place_reactions (
				id,
				user_id,
				provider,
				external_place_id,
				reaction
			)
			VALUES ('00000000-0000-0000-0000-000000000904', ?, 'KTO', '126508', 'LIKE')
			""",
			USER_ID
		);
	}

	private void createExternalContractFixtures() {
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS auth");
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS social");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS auth.user_profiles (
				user_id uuid PRIMARY KEY,
				display_name varchar(80) NOT NULL,
				profile_image_url text
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS social.user_follows (
				follower_user_id uuid NOT NULL,
				following_user_id uuid NOT NULL,
				status varchar(20) NOT NULL,
				deleted_at timestamp with time zone,
				PRIMARY KEY (follower_user_id, following_user_id)
			)
			""");
	}

	@Test
	void excludesReactedPlacesWhenRequested() {
		SwipeFeedResponse response = handler.handle(new SwipeFeedQuery(null, null, 20, true, null));

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().place().externalPlaceId()).isEqualTo("999999");
		assertThat(response.items().getFirst().myReaction()).isNull();
	}

	@Test
	void includesMyReactionWhenRecentPlacesAreNotExcluded() {
		SwipeFeedResponse response = handler.handle(new SwipeFeedQuery(null, "ATTRACTION", 20, false, null));

		assertThat(response.items()).hasSize(2);
		assertThat(response.items().getFirst().place().externalPlaceId()).isEqualTo("126508");
		assertThat(response.items().getFirst().myReaction()).isEqualTo(SwipeReaction.LIKE);
		assertThat(response.items().getFirst().likedByFollowees()).isEmpty();
	}

	@TestConfiguration
	static class CurrentUserTestConfiguration {

		@Bean
		CurrentUserProvider currentUserProvider() {
			return () -> new CurrentUser(USER_ID, "min@example.com");
		}
	}
}
