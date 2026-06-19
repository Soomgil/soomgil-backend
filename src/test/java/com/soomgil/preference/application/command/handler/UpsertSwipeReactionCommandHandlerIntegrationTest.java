package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import java.util.List;
import java.util.Map;
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
	UpsertSwipeReactionCommandHandlerIntegrationTest.CurrentUserTestConfiguration.class
})
@SpringBootTest
class UpsertSwipeReactionCommandHandlerIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");

	@Autowired
	private UpsertSwipeReactionCommandHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
	}

	@Test
	void upsertsFinalReactionAndAppendsSwipeEvents() {
		handler.handle(new UpsertSwipeReactionCommand(PlaceProvider.KTO, "126508", SwipeReaction.LIKE, null));

		SwipeReactionResponse response = handler.handle(new UpsertSwipeReactionCommand(
			PlaceProvider.KTO,
			"126508",
			SwipeReaction.SUPER_LIKE,
			null
		));

		assertThat(response.place().provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(response.place().externalPlaceId()).isEqualTo("126508");
		assertThat(response.reaction()).isEqualTo(SwipeReaction.SUPER_LIKE);
		assertThat(response.savedPlaceEligible()).isTrue();

		Map<String, Object> finalReaction = jdbcTemplate.queryForMap("""
			SELECT user_id, provider, external_place_id, reaction, reaction_count
			FROM preference.user_place_reactions
			WHERE user_id = ?
				AND provider = 'KTO'
				AND external_place_id = '126508'
			""",
			USER_ID
		);

		assertThat(finalReaction)
			.containsEntry("user_id", USER_ID)
			.containsEntry("provider", "KTO")
			.containsEntry("external_place_id", "126508")
			.containsEntry("reaction", "SUPER_LIKE")
			.containsEntry("reaction_count", 2);

		List<Map<String, Object>> events = jdbcTemplate.queryForList("""
			SELECT reaction, previous_reaction
			FROM preference.user_swipe_events
			WHERE user_id = ?
				AND provider = 'KTO'
				AND external_place_id = '126508'
			ORDER BY id
			""",
			USER_ID
		);

		assertThat(events).hasSize(2);
		assertThat(events.get(0))
			.containsEntry("reaction", "LIKE")
			.containsEntry("previous_reaction", null);
		assertThat(events.get(1))
			.containsEntry("reaction", "SUPER_LIKE")
			.containsEntry("previous_reaction", "LIKE");
	}

	@TestConfiguration
	static class CurrentUserTestConfiguration {

		@Bean
		CurrentUserProvider currentUserProvider() {
			return () -> new CurrentUser(USER_ID, "min@example.com");
		}
	}
}
