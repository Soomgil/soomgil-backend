package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecalculateTagStatisticsCommandHandlerIntegrationTest {

	private static final UUID PARK_ENRICHMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000001301");
	private static final UUID MUSEUM_ENRICHMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000001302");

	@Autowired
	private RecalculateTagStatisticsCommandHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanUp();

		insertEnrichment(PARK_ENRICHMENT_ID, "park-place", "park");
		insertEnrichment(MUSEUM_ENRICHMENT_ID, "museum-place", "museum");

		insertFinalReaction("00000000-0000-0000-0000-000000001311", "park-place", "NOPE", PARK_ENRICHMENT_ID);
		insertFinalReaction("00000000-0000-0000-0000-000000001312", "park-place", "LIKE", PARK_ENRICHMENT_ID);
		insertFinalReaction("00000000-0000-0000-0000-000000001313", "park-place", "SUPER_LIKE", PARK_ENRICHMENT_ID);
		insertFinalReaction("00000000-0000-0000-0000-000000001314", "museum-place", "NOPE", MUSEUM_ENRICHMENT_ID);

		insertHistoricalEvent("00000000-0000-0000-0000-000000001311", "park-place", "LIKE", PARK_ENRICHMENT_ID);
		insertHistoricalEvent("00000000-0000-0000-0000-000000001311", "park-place", "SUPER_LIKE", PARK_ENRICHMENT_ID);
		insertHistoricalEvent("00000000-0000-0000-0000-000000001311", "park-place", "NOPE", PARK_ENRICHMENT_ID);
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM preference.tag_statistics");
		jdbcTemplate.update("DELETE FROM preference.tag_statistic_runs");
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_tags");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_candidates");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichments");
	}

	@Test
	void calculatesServingStatisticsFromEachUsersFinalPlaceReaction() {
		var result = handler.handle(new RecalculateTagStatisticsCommand(new BigDecimal("2")));

		Map<String, Object> run = jdbcTemplate.queryForMap("""
			SELECT
				source,
				status,
				alpha,
				global_positive_rate,
				total_reaction_count,
				positive_reaction_count,
				is_serving
			FROM preference.tag_statistic_runs
			WHERE id = ?
			""", result.runId());

		assertThat(run)
			.containsEntry("source", "REAL_USER")
			.containsEntry("status", "SUCCEEDED")
			.containsEntry("total_reaction_count", 4L)
			.containsEntry("positive_reaction_count", 2L)
			.containsEntry("is_serving", true);
		assertThat((BigDecimal) run.get("alpha")).isEqualByComparingTo("2");
		assertThat((BigDecimal) run.get("global_positive_rate")).isEqualByComparingTo("0.500000");

		List<Map<String, Object>> statistics = jdbcTemplate.queryForList("""
			SELECT
				tag.code,
				stat.positive_count,
				stat.reaction_count,
				stat.smoothed_positive_rate,
				stat.preference_discrimination
			FROM preference.tag_statistics stat
			JOIN preference.preference_tags tag ON tag.id = stat.tag_id
			WHERE stat.run_id = ?
			ORDER BY tag.code
			""", result.runId());

		assertThat(statistics).hasSize(2);
		assertStatistic(statistics.get(0), "museum", 0L, 1L, "0.333333", "0.296296");
		assertStatistic(statistics.get(1), "park", 2L, 3L, "0.600000", "0.576000");
	}

	private void insertEnrichment(UUID enrichmentId, String externalPlaceId, String tagCode) {
		UUID tagId = jdbcTemplate.queryForObject(
			"SELECT id FROM preference.preference_tags WHERE code = ?",
			UUID.class,
			tagCode
		);
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichments (
				id, provider, external_place_id, status, selected_count, enriched_at
			)
			VALUES (?, 'KTO', ?, 'SUCCEEDED', 1, now())
			""", enrichmentId, externalPlaceId);
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichment_tags (
				enrichment_id, tag_id, confidence, weight, rank_order
			)
			VALUES (?, ?, 0.9000, 0.8000, 1)
			""", enrichmentId, tagId);
	}

	private void insertFinalReaction(
		String userId,
		String externalPlaceId,
		String reaction,
		UUID enrichmentId
	) {
		jdbcTemplate.update("""
			INSERT INTO preference.user_place_reactions (
				id, user_id, provider, external_place_id, reaction, place_tag_enrichment_id
			)
			VALUES (?, ?, 'KTO', ?, ?, ?)
			""", UUID.randomUUID(), UUID.fromString(userId), externalPlaceId, reaction, enrichmentId);
	}

	private void insertHistoricalEvent(
		String userId,
		String externalPlaceId,
		String reaction,
		UUID enrichmentId
	) {
		jdbcTemplate.update("""
			INSERT INTO preference.user_swipe_events (
				user_id, provider, external_place_id, reaction, place_tag_enrichment_id
			)
			VALUES (?, 'KTO', ?, ?, ?)
			""", UUID.fromString(userId), externalPlaceId, reaction, enrichmentId);
	}

	private void assertStatistic(
		Map<String, Object> statistic,
		String tagCode,
		long positiveCount,
		long reactionCount,
		String smoothedPositiveRate,
		String discrimination
	) {
		assertThat(statistic)
			.containsEntry("code", tagCode)
			.containsEntry("positive_count", positiveCount)
			.containsEntry("reaction_count", reactionCount);
		assertThat((BigDecimal) statistic.get("smoothed_positive_rate"))
			.isEqualByComparingTo(smoothedPositiveRate);
		assertThat((BigDecimal) statistic.get("preference_discrimination"))
			.isEqualByComparingTo(discrimination);
	}
}
