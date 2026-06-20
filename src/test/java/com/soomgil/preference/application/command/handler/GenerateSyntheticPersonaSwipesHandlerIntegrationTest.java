package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.preference.application.command.dto.GenerateSyntheticPersonaSwipesCommand;
import com.soomgil.preference.application.command.dto.GenerateSyntheticPersonaSwipesResult;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsCommand;
import com.soomgil.preference.domain.policy.TagStatisticSource;
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
class GenerateSyntheticPersonaSwipesHandlerIntegrationTest {

	private static final UUID FIRST_ENRICHMENT_ID =
		UUID.fromString("00000000-0000-0000-0000-000000002201");
	private static final UUID SECOND_ENRICHMENT_ID =
		UUID.fromString("00000000-0000-0000-0000-000000002202");

	@Autowired
	private GenerateSyntheticPersonaSwipesHandler handler;

	@Autowired
	private RecalculateTagStatisticsCommandHandler statisticsHandler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanUp();
		insertPlaceEnrichments();
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@Test
	void persistsFiftyPersonasAndDeterministicEventsWithoutTouchingRealEvents() {
		GenerateSyntheticPersonaSwipesResult first = handler.handle(
			new GenerateSyntheticPersonaSwipesCommand(10)
		);
		List<Map<String, Object>> firstEvents = syntheticEvents();

		GenerateSyntheticPersonaSwipesResult second = handler.handle(
			new GenerateSyntheticPersonaSwipesCommand(10)
		);

		assertThat(first.personaCount()).isEqualTo(50);
		assertThat(first.placeCount()).isEqualTo(2);
		assertThat(first.eventCount()).isEqualTo(100);
		assertThat(second.eventCount()).isEqualTo(100);
		assertThat(count("preference.synthetic_personas")).isEqualTo(50);
		assertThat(count("preference.synthetic_swipe_events")).isEqualTo(100);
		assertThat(count("preference.user_swipe_events")).isZero();
		assertThat(syntheticEvents()).containsExactlyElementsOf(firstEvents);
	}

	@Test
	void keepsSyntheticAndRealStatisticsSourcesSeparated() {
		handler.handle(new GenerateSyntheticPersonaSwipesCommand(10));
		insertRealReaction();

		var syntheticResult = statisticsHandler.handle(new RecalculateTagStatisticsCommand(
			new BigDecimal("2"),
			TagStatisticSource.SYNTHETIC_PERSONA,
			"synthetic-persona-v1"
		));

		var realResult = statisticsHandler.handle(new RecalculateTagStatisticsCommand(
			new BigDecimal("2"),
			TagStatisticSource.REAL_USER,
			null,
			true
		));
		Map<String, Object> syntheticRun = statisticRun(syntheticResult.runId());
		Map<String, Object> realRun = statisticRun(realResult.runId());

		assertThat(syntheticRun)
			.containsEntry("source", "SYNTHETIC_PERSONA")
			.containsEntry("total_reaction_count", 100L)
			.containsEntry("is_serving", false);
		assertThat(realRun)
			.containsEntry("source", "REAL_USER")
			.containsEntry("total_reaction_count", 1L)
			.containsEntry("is_serving", true);
	}

	private void insertPlaceEnrichments() {
		UUID quietTagId = tagId("quiet");
		UUID scenicTagId = tagId("scenic_view");
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichments (
				id, provider, external_place_id, status, selected_count, enriched_at
			)
			VALUES
				(?, 'KTO', '1001', 'SUCCEEDED', 2, now()),
				(?, 'KTO', '1002', 'SUCCEEDED', 2, now())
			""", FIRST_ENRICHMENT_ID, SECOND_ENRICHMENT_ID);
		jdbcTemplate.update("""
			INSERT INTO preference.place_tag_enrichment_tags (
				enrichment_id, tag_id, confidence, weight, rank_order
			)
			VALUES
				(?, ?, 1.0, 0.8, 1),
				(?, ?, 1.0, 0.2, 2),
				(?, ?, 1.0, 0.3, 1),
				(?, ?, 1.0, 0.7, 2)
			""",
			FIRST_ENRICHMENT_ID, quietTagId,
			FIRST_ENRICHMENT_ID, scenicTagId,
			SECOND_ENRICHMENT_ID, quietTagId,
			SECOND_ENRICHMENT_ID, scenicTagId
		);
	}

	private List<Map<String, Object>> syntheticEvents() {
		return jdbcTemplate.queryForList("""
			SELECT
				persona_id,
				provider,
				external_place_id,
				reaction,
				generator_version,
				seed,
				persona_place_score
			FROM preference.synthetic_swipe_events
			ORDER BY persona_id, provider, external_place_id
			""");
	}

	private Map<String, Object> statisticRun(UUID runId) {
		return jdbcTemplate.queryForMap("""
			SELECT source, total_reaction_count, is_serving
			FROM preference.tag_statistic_runs
			WHERE id = ?
			""", runId);
	}

	private void insertRealReaction() {
		jdbcTemplate.update("""
			INSERT INTO preference.user_place_reactions (
				id, user_id, provider, external_place_id, reaction, place_tag_enrichment_id
			)
			VALUES (
				'00000000-0000-0000-0000-000000002203',
				'00000000-0000-0000-0000-000000002204',
				'KTO',
				'1001',
				'NOPE',
				?
			)
			""", FIRST_ENRICHMENT_ID);
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}

	private UUID tagId(String code) {
		return jdbcTemplate.queryForObject(
			"SELECT id FROM preference.preference_tags WHERE code = ?",
			UUID.class,
			code
		);
	}

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM preference.tag_statistics");
		jdbcTemplate.update("DELETE FROM preference.tag_statistic_runs");
		jdbcTemplate.update("DELETE FROM preference.synthetic_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.synthetic_persona_tag_preferences");
		jdbcTemplate.update("DELETE FROM preference.synthetic_personas");
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_tags");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_candidates");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichments");
	}
}
