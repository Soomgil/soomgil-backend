package com.soomgil.social.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.social.application.query.dto.FindFolloweePlaceReactionsQuery;
import java.util.List;
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
	FindFolloweePlaceReactionsQueryHandlerIntegrationTest.CurrentUserTestConfiguration.class
})
@SpringBootTest
class FindFolloweePlaceReactionsQueryHandlerIntegrationTest {

	private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000001001");
	private static final UUID LIKED_FOLLOWEE_ID = UUID.fromString("00000000-0000-0000-0000-000000001002");
	private static final UUID NOPE_FOLLOWEE_ID = UUID.fromString("00000000-0000-0000-0000-000000001003");
	private static final UUID PENDING_FOLLOWEE_ID = UUID.fromString("00000000-0000-0000-0000-000000001004");
	private static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000001005");

	@Autowired
	private FindFolloweePlaceReactionsQueryHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		createExternalContractFixtures();
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
		jdbcTemplate.update("DELETE FROM social.user_follows");
		jdbcTemplate.update("DELETE FROM auth.user_profiles");
		jdbcTemplate.update(
			"INSERT INTO auth.users (id) VALUES (?) ON CONFLICT (id) DO NOTHING",
			CURRENT_USER_ID
		);

		insertProfile(LIKED_FOLLOWEE_ID, "좋아요 친구", "https://cdn.soomgil.example.com/users/liked.jpg");
		insertProfile(NOPE_FOLLOWEE_ID, "싫어요 친구", null);
		insertProfile(PENDING_FOLLOWEE_ID, "승인 대기 친구", null);
		insertProfile(STRANGER_ID, "팔로우하지 않은 사용자", null);

		insertFollow(LIKED_FOLLOWEE_ID, "ACTIVE");
		insertFollow(NOPE_FOLLOWEE_ID, "ACTIVE");
		insertFollow(PENDING_FOLLOWEE_ID, "PENDING");

		insertReaction(LIKED_FOLLOWEE_ID, "126508", "SUPER_LIKE");
		insertReaction(NOPE_FOLLOWEE_ID, "126508", "NOPE");
		insertReaction(PENDING_FOLLOWEE_ID, "126508", "LIKE");
		insertReaction(STRANGER_ID, "126508", "LIKE");
	}

	@Test
	void returnsOnlyActiveFolloweesWithPositiveReactions() {
		var reactions = handler.handle(new FindFolloweePlaceReactionsQuery(List.of(
			new PlaceRef(PlaceProvider.KTO, "126508")
		)));

		assertThat(reactions).hasSize(1);
		assertThat(reactions.getFirst().place())
			.isEqualTo(new PlaceRef(PlaceProvider.KTO, "126508"));
		assertThat(reactions.getFirst().followee().id()).isEqualTo(LIKED_FOLLOWEE_ID);
		assertThat(reactions.getFirst().followee().displayName()).isEqualTo("좋아요 친구");
		assertThat(reactions.getFirst().followee().profileImageUrl())
			.hasToString("https://cdn.soomgil.example.com/users/liked.jpg");
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

	private void insertProfile(UUID userId, String displayName, String profileImageUrl) {
		jdbcTemplate.update("INSERT INTO auth.users (id) VALUES (?) ON CONFLICT (id) DO NOTHING", userId);
		jdbcTemplate.update("""
			INSERT INTO auth.user_profiles (user_id, display_name, profile_image_url)
			VALUES (?, ?, ?)
			""", userId, displayName, profileImageUrl);
	}

	private void insertFollow(UUID followingUserId, String status) {
		jdbcTemplate.update("""
			INSERT INTO social.user_follows (follower_user_id, following_user_id, status)
			VALUES (?, ?, ?)
			""", CURRENT_USER_ID, followingUserId, status);
	}

	private void insertReaction(UUID userId, String externalPlaceId, String reaction) {
		jdbcTemplate.update("""
			INSERT INTO preference.user_place_reactions (
				id,
				user_id,
				provider,
				external_place_id,
				reaction
			)
			VALUES (?, ?, 'KTO', ?, ?)
			""", UUID.randomUUID(), userId, externalPlaceId, reaction);
	}

	@TestConfiguration
	static class CurrentUserTestConfiguration {

		@Bean
		CurrentUserProvider currentUserProvider() {
			return () -> new CurrentUser(CURRENT_USER_ID, "min@example.com");
		}
	}
}
