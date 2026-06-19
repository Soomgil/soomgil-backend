package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.query.dto.PlaceImageCandidate;
import com.soomgil.place.application.query.dto.PlaceImageCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceImageCandidateType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PlaceImageCandidateQueryHandlerIntegrationTest {

	@Autowired
	private PlaceImageCandidateQueryHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM tourism_source.contest_award_photo_matches");
		jdbcTemplate.update("DELETE FROM tourism_source.contest_award_photos");
		jdbcTemplate.update("DELETE FROM tourism_source.photo_contests");
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
			VALUES (
				1,
				126508,
				'Haeundae Beach',
				12,
				26,
				2,
				35.1587,
				129.1604,
				'Busan Haeundae-gu',
				'2026-06-01T00:00:00Z'::timestamptz,
				'2026-06-02T00:00:00Z'::timestamptz
			)
			""");
		for (int order = 1; order <= 5; order++) {
			insertNormalImage(order, true);
		}
		insertNormalImage(0, false);
	}

	@Test
	void combinesFourNormalImagesAndOneExactAwardImage() {
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
			"exact-award.jpg",
			"https://cdn.soomgil.example.com/awards/exact.jpg",
			2024,
			3,
			"UPLOADED",
			"APPROVED",
			"2026-06-01T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000201"),
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
			"ATTRACTION",
			1,
			null,
			null,
			"SELECTED",
			true
		);
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000102"),
			"region-award.jpg",
			"https://cdn.soomgil.example.com/awards/region.jpg",
			2026,
			1,
			"UPLOADED",
			"APPROVED",
			"2026-06-02T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000202"),
			UUID.fromString("00000000-0000-0000-0000-000000000102"),
			"REGION",
			null,
			26,
			2,
			"SELECTED",
			true
		);

		List<PlaceImageCandidate> images = handler.handle(new PlaceImageCandidateQuery(PlaceProvider.KTO, "126508"));

		assertThat(images).hasSize(5);
		assertThat(images.subList(0, 4))
			.extracting(PlaceImageCandidate::type)
			.containsOnly(PlaceImageCandidateType.GENERAL);
		assertThat(images.subList(0, 4))
			.extracting(image -> image.publicUrl().toString())
			.containsExactly(
				"https://cdn.soomgil.example.com/places/general-1.jpg",
				"https://cdn.soomgil.example.com/places/general-2.jpg",
				"https://cdn.soomgil.example.com/places/general-3.jpg",
				"https://cdn.soomgil.example.com/places/general-4.jpg"
			);
		assertThat(images.get(4).type()).isEqualTo(PlaceImageCandidateType.AWARD);
		assertThat(images.get(4).publicUrl()).hasToString("https://cdn.soomgil.example.com/awards/exact.jpg");
	}

	@Test
	void fallsBackToBestRegionAwardWhenExactAwardDoesNotExist() {
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000111"),
			"older-region-award.jpg",
			"https://cdn.soomgil.example.com/awards/older-region.jpg",
			2024,
			1,
			"UPLOADED",
			"APPROVED",
			"2026-06-01T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000211"),
			UUID.fromString("00000000-0000-0000-0000-000000000111"),
			"REGION",
			null,
			26,
			2,
			"SELECTED",
			true
		);
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000112"),
			"best-region-award.jpg",
			"https://cdn.soomgil.example.com/awards/best-region.jpg",
			2026,
			1,
			"UPLOADED",
			"APPROVED",
			"2026-06-01T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000212"),
			UUID.fromString("00000000-0000-0000-0000-000000000112"),
			"REGION",
			null,
			26,
			2,
			"SELECTED",
			true
		);
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000113"),
			"lower-rank-region-award.jpg",
			"https://cdn.soomgil.example.com/awards/lower-rank-region.jpg",
			2026,
			5,
			"UPLOADED",
			"APPROVED",
			"2026-06-02T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000213"),
			UUID.fromString("00000000-0000-0000-0000-000000000113"),
			"REGION",
			null,
			26,
			2,
			"SELECTED",
			true
		);

		List<PlaceImageCandidate> images = handler.handle(new PlaceImageCandidateQuery(PlaceProvider.KTO, "126508"));

		assertThat(images).hasSize(5);
		assertThat(images.get(4).type()).isEqualTo(PlaceImageCandidateType.AWARD);
		assertThat(images.get(4).publicUrl()).hasToString("https://cdn.soomgil.example.com/awards/best-region.jpg");
	}

	@Test
	void excludesNotPublicAwardPhotosAndReturnsOnlyNormalImages() {
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000121"),
			"rejected-award.jpg",
			"https://cdn.soomgil.example.com/awards/rejected.jpg",
			2026,
			1,
			"UPLOADED",
			"REJECTED",
			"2026-06-01T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000221"),
			UUID.fromString("00000000-0000-0000-0000-000000000121"),
			"ATTRACTION",
			1,
			null,
			null,
			"SELECTED",
			true
		);
		insertAwardPhoto(
			UUID.fromString("00000000-0000-0000-0000-000000000122"),
			"unmatched-award.jpg",
			"https://cdn.soomgil.example.com/awards/unmatched.jpg",
			2026,
			1,
			"UPLOADED",
			"APPROVED",
			"2026-06-02T00:00:00Z"
		);
		insertAwardMatch(
			UUID.fromString("00000000-0000-0000-0000-000000000222"),
			UUID.fromString("00000000-0000-0000-0000-000000000122"),
			"UNMATCHED",
			null,
			null,
			null,
			"SELECTED",
			true
		);

		List<PlaceImageCandidate> images = handler.handle(new PlaceImageCandidateQuery(PlaceProvider.KTO, "126508"));

		assertThat(images).hasSize(4);
		assertThat(images)
			.extracting(PlaceImageCandidate::type)
			.containsOnly(PlaceImageCandidateType.GENERAL);
	}

	private void insertNormalImage(int displayOrder, boolean active) {
		jdbcTemplate.update("""
			INSERT INTO tourism_source.attraction_images (
				id,
				attraction_no,
				source_type,
				public_url,
				display_order,
				is_active
			)
			VALUES (?, 1, 'DETAIL_IMAGE', ?, ?, ?)
			""",
			UUID.fromString("00000000-0000-0000-0000-0000000000" + String.format("%02d", displayOrder + 10)),
			"https://cdn.soomgil.example.com/places/general-" + displayOrder + ".jpg",
			displayOrder,
			active
		);
	}

	private void insertAwardPhoto(
		UUID id,
		String fileName,
		String publicUrl,
		int awardYear,
		int awardRank,
		String uploadStatus,
		String rightsStatus,
		String createdAt
	) {
		jdbcTemplate.update("""
			INSERT INTO tourism_source.contest_award_photos (
				id,
				award_year,
				award_rank,
				original_file_name,
				object_key,
				public_url,
				upload_status,
				rights_status,
				created_at,
				updated_at
			)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz)
			""",
			id,
			awardYear,
			awardRank,
			fileName,
			"awards/" + fileName,
			publicUrl,
			uploadStatus,
			rightsStatus,
			createdAt,
			createdAt
		);
	}

	private void insertAwardMatch(
		UUID id,
		UUID photoId,
		String matchScope,
		Integer attractionNo,
		Integer sidoCode,
		Integer gugunCode,
		String matchStatus,
		boolean selected
	) {
		jdbcTemplate.update("""
			INSERT INTO tourism_source.contest_award_photo_matches (
				id,
				photo_id,
				attraction_no,
				sido_code,
				gugun_code,
				match_scope,
				match_status,
				match_method,
				confidence,
				is_selected
			)
			VALUES (?, ?, ?, ?, ?, ?, ?, 'MANUAL', 1.0000, ?)
			""",
			id,
			photoId,
			attractionNo,
			sidoCode,
			gugunCode,
			matchScope,
			matchStatus,
			selected
		);
	}
}
