package com.soomgil.tourismsource.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ContestAwardPhotoMatchingCommandHandlerIntegrationTest {

	private static final UUID PHOTO_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

	@Autowired
	private ContestAwardPhotoMatchingCommandHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM tourism_source.contest_award_photo_matches");
		jdbcTemplate.update("DELETE FROM tourism_source.contest_award_photos");
		jdbcTemplate.update("DELETE FROM tourism_source.region_aliases");
		jdbcTemplate.update("DELETE FROM tourism_source.attraction_images");
		jdbcTemplate.update("DELETE FROM tourism_source.attractions");
		jdbcTemplate.update("DELETE FROM tourism_source.guguns");
		jdbcTemplate.update("DELETE FROM tourism_source.sidos");
		jdbcTemplate.update("DELETE FROM tourism_source.contenttypes");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.contenttypes (content_type_id, content_type_name)
			VALUES (12, 'ATTRACTION')
			""");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.sidos (sido_code, sido_name)
			VALUES (26, 'Busan')
			""");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.guguns (sido_code, gugun_code, gugun_name)
			VALUES (26, 2, 'Haeundae-gu')
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
				addr1
			)
			VALUES (1, 126508, 'Haeundae Beach', 12, 26, 2, 35.1587, 129.1604, 'Busan Haeundae-gu')
			""");
	}

	@Test
	void createsRegionCandidateFromAliasText() {
		insertRegionAlias("해운대", "해운대", 26, 2);
		insertPhoto("Busan night view", "busan_haeundae_2024.jpg", "부산 해운대");

		ContestAwardPhotoMatchingResult result = handler.handle(new ContestAwardPhotoMatchingCommand(PHOTO_ID));

		assertThat(result.createdMatches()).isEqualTo(1);
		assertThat(result.selectedMatches()).isZero();
		List<Map<String, Object>> matches = matches();
		assertThat(matches).hasSize(1);
		assertThat(matches.getFirst())
			.containsEntry("match_scope", "REGION")
			.containsEntry("match_status", "CANDIDATE")
			.containsEntry("match_method", "FILE_NAME_REGION")
			.containsEntry("is_selected", false)
			.containsEntry("sido_code", 26)
			.containsEntry("gugun_code", 2);
	}

	@Test
	void selectsSingleHighConfidenceAttractionMatch() {
		insertPhoto("Haeundae Beach", "haeundae_beach_award.jpg", null);

		ContestAwardPhotoMatchingResult result = handler.handle(new ContestAwardPhotoMatchingCommand(PHOTO_ID));

		assertThat(result.createdMatches()).isEqualTo(1);
		assertThat(result.selectedMatches()).isEqualTo(1);
		List<Map<String, Object>> matches = matches();
		assertThat(matches).hasSize(1);
		assertThat(matches.getFirst())
			.containsEntry("match_scope", "ATTRACTION")
			.containsEntry("match_status", "SELECTED")
			.containsEntry("match_method", "TITLE_TEXT")
			.containsEntry("is_selected", true)
			.containsEntry("attraction_no", 1);
		assertThat((BigDecimal) matches.getFirst().get("confidence")).isGreaterThanOrEqualTo(new BigDecimal("0.9000"));
	}

	@Test
	void createsUnmatchedCandidateWhenNoRuleMatches() {
		insertPhoto("Unknown landscape", "unknown_landscape.jpg", "unknown");

		ContestAwardPhotoMatchingResult result = handler.handle(new ContestAwardPhotoMatchingCommand(PHOTO_ID));

		assertThat(result.createdMatches()).isEqualTo(1);
		assertThat(result.selectedMatches()).isZero();
		List<Map<String, Object>> matches = matches();
		assertThat(matches).hasSize(1);
		assertThat(matches.getFirst())
			.containsEntry("match_scope", "UNMATCHED")
			.containsEntry("match_status", "CANDIDATE")
			.containsEntry("match_method", "IMPORT_METADATA")
			.containsEntry("is_selected", false);
	}

	private void insertRegionAlias(String alias, String normalizedAlias, int sidoCode, int gugunCode) {
		jdbcTemplate.update("""
			INSERT INTO tourism_source.region_aliases (
				id,
				alias,
				normalized_alias,
				sido_code,
				gugun_code,
				alias_type
			)
			VALUES ('00000000-0000-0000-0000-000000000401', ?, ?, ?, ?, 'MANUAL')
			""",
			alias,
			normalizedAlias,
			sidoCode,
			gugunCode
		);
	}

	private void insertPhoto(String title, String originalFileName, String extractedRegionText) {
		jdbcTemplate.update("""
			INSERT INTO tourism_source.contest_award_photos (
				id,
				award_year,
				award_rank,
				title,
				original_file_name,
				extracted_region_text,
				object_key,
				public_url,
				upload_status,
				rights_status
			)
			VALUES (?, 2026, 1, ?, ?, ?, 'awards/' || ?, 'https://cdn.soomgil.example.com/awards/' || ?, 'UPLOADED', 'APPROVED')
			""",
			PHOTO_ID,
			title,
			originalFileName,
			extractedRegionText,
			originalFileName,
			originalFileName
		);
	}

	private List<Map<String, Object>> matches() {
		return jdbcTemplate.queryForList("""
			SELECT
				match_scope,
				match_status,
				match_method,
				confidence,
				is_selected,
				attraction_no,
				sido_code,
				gugun_code
			FROM tourism_source.contest_award_photo_matches
			WHERE photo_id = ?
			ORDER BY created_at, id
			""",
			PHOTO_ID
		);
	}
}
