package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.preference.application.command.dto.SavePlaceTagCandidateCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentResult;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SavePlaceTagEnrichmentCommandHandlerIntegrationTest {

	@Autowired
	private SavePlaceTagEnrichmentCommandHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_tags");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichment_candidates");
		jdbcTemplate.update("DELETE FROM preference.place_tag_enrichments");
	}

	@Test
	void savesCandidatesAndOnlyWhitelistedSelectedTags() {
		SavePlaceTagEnrichmentResult result = handler.handle(new SavePlaceTagEnrichmentCommand(
			"KTO",
			"126508",
			OffsetDateTime.parse("2026-06-01T00:00:00Z"),
			"source-hash-126508",
			"openai",
			"gpt-5-mini",
			"place-tagging-v1",
			"selection-v1",
			List.of(
				new SavePlaceTagCandidateCommand(
					"park",
					new BigDecimal("0.9200"),
					new BigDecimal("0.8000"),
					false,
					"Place has a clear park context."
				),
				new SavePlaceTagCandidateCommand(
					"unknown_tag",
					new BigDecimal("0.9900"),
					new BigDecimal("0.7000"),
					true,
					"Model emitted a tag outside the fixed whitelist."
				)
			)
		));

		assertThat(result.candidateCount()).isEqualTo(2);
		assertThat(result.selectedCount()).isEqualTo(1);

		Map<String, Object> enrichment = jdbcTemplate.queryForMap("""
			SELECT
				provider,
				external_place_id,
				status,
				model_provider,
				model_name,
				prompt_version,
				tag_dictionary_version,
				selection_policy_version,
				candidate_count,
				selected_count
			FROM preference.place_tag_enrichments
			WHERE id = ?
			""",
			result.enrichmentId()
		);

		assertThat(enrichment)
			.containsEntry("provider", "KTO")
			.containsEntry("external_place_id", "126508")
			.containsEntry("status", "SUCCEEDED")
			.containsEntry("model_provider", "openai")
			.containsEntry("model_name", "gpt-5-mini")
			.containsEntry("prompt_version", "place-tagging-v1")
			.containsEntry("tag_dictionary_version", "preference-tags-v1")
			.containsEntry("selection_policy_version", "selection-v1")
			.containsEntry("candidate_count", 2)
			.containsEntry("selected_count", 1);

		List<Map<String, Object>> candidates = jdbcTemplate.queryForList("""
			SELECT candidate_code, confidence, weight, status
			FROM preference.place_tag_enrichment_candidates
			WHERE enrichment_id = ?
			ORDER BY candidate_code
			""",
			result.enrichmentId()
		);

		assertThat(candidates).hasSize(2);
		assertThat(candidates.get(0))
			.containsEntry("candidate_code", "park")
			.containsEntry("status", "SELECTED");
		assertThat((BigDecimal) candidates.get(0).get("confidence")).isEqualByComparingTo("0.9200");
		assertThat((BigDecimal) candidates.get(0).get("weight")).isEqualByComparingTo("0.8000");
		assertThat(candidates.get(1))
			.containsEntry("candidate_code", "unknown_tag")
			.containsEntry("status", "REJECTED_OUT_OF_DICTIONARY");

		List<Map<String, Object>> selectedTags = jdbcTemplate.queryForList("""
			SELECT
				t.code,
				et.confidence,
				et.weight,
				et.preference_discrimination_snapshot,
				et.selection_score,
				et.rank_order
			FROM preference.place_tag_enrichment_tags et
			JOIN preference.preference_tags t ON t.id = et.tag_id
			WHERE et.enrichment_id = ?
			""",
			result.enrichmentId()
		);

		assertThat(selectedTags).hasSize(1);
		assertThat(selectedTags.getFirst())
			.containsEntry("code", "park")
			.containsEntry("rank_order", 1);
		assertThat((BigDecimal) selectedTags.getFirst().get("confidence")).isEqualByComparingTo("0.9200");
		assertThat((BigDecimal) selectedTags.getFirst().get("weight")).isEqualByComparingTo("0.8000");
		assertThat((BigDecimal) selectedTags.getFirst().get("preference_discrimination_snapshot"))
			.isEqualByComparingTo("0.500000");
		assertThat((BigDecimal) selectedTags.getFirst().get("selection_score"))
			.isEqualByComparingTo("0.800000");
	}
}
