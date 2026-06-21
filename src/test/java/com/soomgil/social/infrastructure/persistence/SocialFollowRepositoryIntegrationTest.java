package com.soomgil.social.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.social.application.SocialFollowService;
import com.soomgil.social.api.dto.FollowStatus;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SocialFollowRepositoryIntegrationTest {

	private static final UUID CURRENT = UUID.fromString("10000000-0000-0000-0000-000000000011");
	private static final UUID PUBLIC_USER = UUID.fromString("10000000-0000-0000-0000-000000000012");
	private static final UUID PRIVATE_USER = UUID.fromString("10000000-0000-0000-0000-000000000013");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SocialFollowService service;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS auth");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS auth.user_profiles (
				user_id uuid PRIMARY KEY,
				display_name varchar(80) NOT NULL,
				profile_image_url text,
				profile_visibility varchar(20) NOT NULL DEFAULT 'PUBLIC'
			)
			""");
		jdbcTemplate.execute("ALTER TABLE auth.user_profiles ADD COLUMN IF NOT EXISTS profile_visibility varchar(20) NOT NULL DEFAULT 'PUBLIC'");
		jdbcTemplate.update("DELETE FROM social.user_follows");
		jdbcTemplate.update("DELETE FROM auth.user_profiles");
		insertProfile(CURRENT, "Current", "PRIVATE");
		insertProfile(PUBLIC_USER, "Public", "PUBLIC");
		insertProfile(PRIVATE_USER, "Private", "PRIVATE");
	}

	@Test
	void persistsPublicFollowAndPrivateApprovalFlow() {
		assertThat(service.follow(CURRENT, PUBLIC_USER).status()).isEqualTo(FollowStatus.ACTIVE);
		assertThat(service.follow(CURRENT, PRIVATE_USER).status()).isEqualTo(FollowStatus.PENDING);

		assertThat(service.listPending(PRIVATE_USER, 0, 20).items()).hasSize(1);
		assertThat(service.accept(PRIVATE_USER, CURRENT).status()).isEqualTo(FollowStatus.ACTIVE);

		service.unfollow(CURRENT, PUBLIC_USER);
		assertThat(jdbcTemplate.queryForObject(
			"SELECT status FROM social.user_follows WHERE follower_user_id = ? AND following_user_id = ?",
			String.class, CURRENT, PUBLIC_USER
		)).isEqualTo("DELETED");
	}

	@Test
	void pagesPublicListsAndProtectsPrivateLists() {
		service.follow(CURRENT, PUBLIC_USER);

		assertThat(service.listFollowers(null, PUBLIC_USER, 0, 20).items())
			.extracting("displayName")
			.containsExactly("Current");
		assertThat(service.listFollowing(CURRENT, CURRENT, 0, 20).items())
			.extracting("displayName")
			.containsExactly("Public");
		assertThatThrownBy(() -> service.listFollowing(null, CURRENT, 0, 20))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
	}

	private void insertProfile(UUID userId, String name, String visibility) {
		jdbcTemplate.update("INSERT INTO auth.users (id) VALUES (?) ON CONFLICT (id) DO NOTHING", userId);
		jdbcTemplate.update(
			"INSERT INTO auth.user_profiles (user_id, display_name, profile_visibility) VALUES (?, ?, ?)",
			userId, name, visibility
		);
	}
}
