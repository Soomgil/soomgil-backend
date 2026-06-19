package com.soomgil.tourismsource.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ContestAwardPhotoMatchingBatchRunnerIntegrationTest {

	private static final UUID EXACT_PHOTO_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
	private static final UUID REGION_PHOTO_ID = UUID.fromString("00000000-0000-0000-0000-000000000502");

	@Autowired
	private ContestAwardPhotoMatchingBatchService batchService;

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
			INSERT INTO tourism_source.region_aliases (
				id,
				alias,
				normalized_alias,
				sido_code,
				gugun_code,
				alias_type
			)
			VALUES ('00000000-0000-0000-0000-000000000601', '해운대', '해운대', 26, 2, 'MANUAL')
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
		insertPhoto(EXACT_PHOTO_ID, "Haeundae Beach", "haeundae-beach.jpg", null);
		insertPhoto(REGION_PHOTO_ID, "Busan night view", "busan-haeundae.jpg", "부산 해운대");
	}

	@Test
	void applicationRunnerProcessesPendingAwardPhotos() {
		ContestAwardPhotoMatchingApplicationRunner runner = new ContestAwardPhotoMatchingApplicationRunner(
			batchService,
			100
		);

		runner.run(new DefaultApplicationArguments());

		List<Map<String, Object>> matches = jdbcTemplate.queryForList("""
			SELECT
				photo_id,
				match_scope,
				match_status,
				is_selected
			FROM tourism_source.contest_award_photo_matches
			ORDER BY photo_id
			""");

		assertThat(matches).hasSize(2);
		assertThat(matches.get(0))
			.containsEntry("match_scope", "ATTRACTION")
			.containsEntry("match_status", "SELECTED")
			.containsEntry("is_selected", true);
		assertThat(matches.get(1))
			.containsEntry("match_scope", "REGION")
			.containsEntry("match_status", "CANDIDATE")
			.containsEntry("is_selected", false);
	}

	@Test
	void batchServiceSkipsAlreadyMatchedPhotos() {
		ContestAwardPhotoMatchingBatchResult first = batchService.runPending(100);
		ContestAwardPhotoMatchingBatchResult second = batchService.runPending(100);

		assertThat(first.processedPhotos()).isEqualTo(2);
		assertThat(second.processedPhotos()).isZero();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT count(*) FROM tourism_source.contest_award_photo_matches",
			Integer.class
		)).isEqualTo(2);
	}

	private void insertPhoto(UUID id, String title, String originalFileName, String extractedRegionText) {
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
			id,
			title,
			originalFileName,
			extractedRegionText,
			originalFileName,
			originalFileName
		);
	}
}
