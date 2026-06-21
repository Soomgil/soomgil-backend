package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
	SwipeReactionPreferenceEvidenceIntegrationTest.CurrentUserTestConfiguration.class
})
@SpringBootTest
class SwipeReactionPreferenceEvidenceIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000001401");
	private static final UUID ENRICHMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000001402");
	private static final UUID STATISTIC_RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000001403");

	@Autowired
	private PreferenceUpsertSwipeReactionCommandHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanUp();
		UUID parkTagId = findTagId("park");
		UUID museumTagId = findTagId("museum");
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichments (
				id, provider, external_place_id, status, selected_count, enriched_at
			)
			VALUES (?, 'KTO', '126508', 'SUCCEEDED', 2, now())
			""", ENRICHMENT_ID);
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichment_tags (
				enrichment_id, tag_id, confidence, weight, rank_order
			)
			VALUES
				(?, ?, 0.8000, 0.5000, 1),
				(?, ?, 0.6000, 1.0000, 2)
			""", ENRICHMENT_ID, parkTagId, ENRICHMENT_ID, museumTagId);
		jdbcTemplate.update("""
			INSERT INTO preference.tag_statistic_runs (
				id, source, status, alpha, global_positive_rate,
				total_reaction_count, positive_reaction_count, is_serving
			)
			VALUES (?, 'REAL_USER', 'SUCCEEDED', 100, 0.500000, 1000, 500, true)
			""", STATISTIC_RUN_ID);
		jdbcTemplate.update("""
			INSERT INTO preference.tag_statistics (
				run_id, tag_id, preference_discrimination, smoothed_positive_rate,
				positive_count, reaction_count
			)
			VALUES
				(?, ?, 0.800000, 0.600000, 60, 100),
				(?, ?, 0.200000, 0.400000, 40, 100)
			""", STATISTIC_RUN_ID, parkTagId, STATISTIC_RUN_ID, museumTagId);
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@Test
	void keepsOnlyTheLatestReactionEvidenceForEachPlace() {
		react(SwipeReaction.LIKE);
		assertEvidence("park", "0.40000000", "0.00000000", "0.846154", 1, 0, 0);
		assertEvidence("museum", "0.60000000", "0.00000000", "0.478261", 1, 0, 0);

		react(SwipeReaction.SUPER_LIKE);
		assertEvidence("park", "0.80000000", "0.00000000", "0.904762", 0, 1, 0);
		assertEvidence("museum", "1.20000000", "0.00000000", "0.538462", 0, 1, 0);

		react(SwipeReaction.NOPE);
		assertEvidence("park", "0.00000000", "0.40000000", "0.230769", 0, 0, 1);
		assertEvidence("museum", "0.00000000", "0.60000000", "0.347826", 0, 0, 1);

		react(SwipeReaction.NOPE);
		assertEvidence("park", "0.00000000", "0.40000000", "0.230769", 0, 0, 1);
		assertEvidence("museum", "0.00000000", "0.60000000", "0.347826", 0, 0, 1);

		Integer eventCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM preference.user_swipe_events
			WHERE user_id = ?
			""", Integer.class, USER_ID);
		assertThat(eventCount).isEqualTo(4);

		UUID linkedEnrichmentId = jdbcTemplate.queryForObject("""
			SELECT place_tag_enrichment_id
			FROM preference.user_place_reactions
			WHERE user_id = ?
				AND provider = 'KTO'
				AND external_place_id = '126508'
			""", UUID.class, USER_ID);
		assertThat(linkedEnrichmentId).isEqualTo(ENRICHMENT_ID);
	}

	@Test
	void reappliesAStoredReactionWhenAsyncTaggingFinishes() {
		react(SwipeReaction.LIKE);
		UUID refreshedEnrichmentId = UUID.fromString("00000000-0000-0000-0000-000000001404");
		UUID parkTagId = findTagId("park");
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichments (
				id, provider, external_place_id, status, selected_count, enriched_at
			) VALUES (?, 'KTO', '126508', 'SUCCEEDED', 1, now() + interval '1 second')
			""", refreshedEnrichmentId);
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichment_tags (
				enrichment_id, tag_id, confidence, weight, rank_order
			) VALUES (?, ?, 1.0000, 1.0000, 1)
			""", refreshedEnrichmentId, parkTagId);

		handler.refreshPlaceEnrichment("KTO", "126508", null);

		UUID linked = jdbcTemplate.queryForObject("""
			SELECT place_tag_enrichment_id FROM preference.user_place_reactions
			WHERE user_id = ? AND provider = 'KTO' AND external_place_id = '126508'
			""", UUID.class, USER_ID);
		assertThat(linked).isEqualTo(refreshedEnrichmentId);
		assertEvidence("park", "1.00000000", "0.00000000", "0.920000", 1, 0, 0);
		assertThat(jdbcTemplate.queryForObject(
			"SELECT count(*) FROM preference.user_swipe_events WHERE user_id = ?", Integer.class, USER_ID
		)).isEqualTo(1);
	}

	private void react(SwipeReaction reaction) {
		handler.handle(new UpsertSwipeReactionCommand(
			PlaceProvider.KTO,
			"126508",
			reaction,
			null
		));
	}

	private void assertEvidence(
		String tagCode,
		String positiveEvidence,
		String negativeEvidence,
		String preferenceScore,
		int likeCount,
		int superLikeCount,
		int nopeCount
	) {
		Map<String, Object> row = jdbcTemplate.queryForMap("""
			SELECT
				weight.positive_evidence,
				weight.negative_evidence,
				weight.preference_score,
				weight.like_count,
				weight.super_like_count,
				weight.nope_count
			FROM preference.user_preference_tag_weights weight
			JOIN preference.preference_tags tag ON tag.id = weight.tag_id
			WHERE weight.user_id = ?
				AND tag.code = ?
			""", USER_ID, tagCode);

		assertThat((BigDecimal) row.get("positive_evidence")).isEqualByComparingTo(positiveEvidence);
		assertThat((BigDecimal) row.get("negative_evidence")).isEqualByComparingTo(negativeEvidence);
		assertThat((BigDecimal) row.get("preference_score")).isEqualByComparingTo(preferenceScore);
		assertThat(row)
			.containsEntry("like_count", likeCount)
			.containsEntry("super_like_count", superLikeCount)
			.containsEntry("nope_count", nopeCount);
	}

	private UUID findTagId(String code) {
		return jdbcTemplate.queryForObject(
			"SELECT id FROM preference.preference_tags WHERE code = ?",
			UUID.class,
			code
		);
	}

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM preference.user_preference_tag_weights");
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
		jdbcTemplate.update("DELETE FROM preference.tag_statistics");
		jdbcTemplate.update("DELETE FROM preference.tag_statistic_runs");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_tags");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_candidates");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichments");
	}

	@TestConfiguration
	static class CurrentUserTestConfiguration {

		@Bean
		CurrentUserProvider currentUserProvider() {
			return () -> new CurrentUser(USER_ID, "min@example.com");
		}
	}
}
