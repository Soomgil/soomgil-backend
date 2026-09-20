package com.soomgil.preference;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class JejuPlaceDataSeedIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void seedsNationwideTagEnrichmentsFromMigrations() {
		Integer enrichmentCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM preference.place_tag_enrichments
			WHERE provider = 'KTO'
				AND prompt_version = 'jeju-place-text-tagging-v1'
				AND status = 'SUCCEEDED'
			""", Integer.class);
		Integer selectedTagCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM preference.place_tag_enrichments enrichment
			JOIN preference.place_tag_enrichment_tags tag
				ON tag.enrichment_id = enrichment.id
			WHERE enrichment.provider = 'KTO'
				AND enrichment.prompt_version = 'jeju-place-text-tagging-v1'
				AND enrichment.status = 'SUCCEEDED'
			""", Integer.class);

		// V40의 제주 2,335건에 V55 전국 데이터가 더해진 현재 기준이다.
		assertThat(enrichmentCount).isGreaterThanOrEqualTo(50_000);
		assertThat(selectedTagCount).isGreaterThanOrEqualTo(4_701);
	}

	@Test
	void seedsJejuAccessibilityOverridesForWheelchairMapping() {
		Integer overrideCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM tourism_source.place_accessibility_overrides
			WHERE source_name = 'jeju-accessibility-seed-v1'
			""", Integer.class);
		String osullocFlags = jdbcTemplate.queryForObject("""
			SELECT flags_csv
			FROM tourism_source.place_accessibility_overrides
			WHERE provider = 'KTO' AND external_place_id = '2660802'
			""", String.class);
		String seongsanUnavailable = jdbcTemplate.queryForObject("""
			SELECT unavailable_flags_csv
			FROM tourism_source.place_accessibility_overrides
			WHERE provider = 'KTO' AND external_place_id = '126435'
			""", String.class);

		assertThat(overrideCount).isGreaterThanOrEqualTo(40);
		assertThat(osullocFlags).contains("WHEELCHAIR", "DISABLED_TOILET");
		assertThat(seongsanUnavailable).contains("WHEELCHAIR");
	}

	@Test
	void seedsJejuTourismSourceAttractionsForSearchAndRecommendation() {
		Integer placeCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM tourism_source.attractions
			WHERE area_code = 39
			""", Integer.class);
		Integer imageCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM tourism_source.attraction_images image
			JOIN tourism_source.attractions attraction
				ON attraction.no = image.attraction_no
			WHERE attraction.area_code = 39
				AND image.is_active = true
			""", Integer.class);
		Integer taggedPlaceCount = jdbcTemplate.queryForObject("""
			SELECT count(*)
			FROM tourism_source.attractions attraction
			JOIN preference.place_tag_enrichments enrichment
				ON enrichment.provider = 'KTO'
				AND enrichment.external_place_id = attraction.content_id::text
			WHERE attraction.area_code = 39
				AND enrichment.status = 'SUCCEEDED'
			""", Integer.class);
		String seongsanTitle = jdbcTemplate.queryForObject("""
			SELECT title
			FROM tourism_source.attractions
			WHERE content_id = 126435
			""", String.class);

		assertThat(placeCount).isGreaterThanOrEqualTo(80);
		// V59는 실제 원천에 이미지가 있는 관광지만 보존한다.
		assertThat(imageCount).isGreaterThanOrEqualTo(40);
		assertThat(taggedPlaceCount).isGreaterThanOrEqualTo(40);
		assertThat(seongsanTitle).startsWith("성산일출봉");
	}
}
