package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PlaceViewportCandidateQueryHandlerIntegrationTest {

	@Autowired
	private PlaceViewportCandidateQueryHandler handler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
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
			VALUES
				(1, 126508, 'Haeundae Beach', 12, 26, 0, 35.1587, 129.1604, 'Busan Haeundae-gu', '2026-06-01T00:00:00Z', '2026-06-02T00:00:00Z'),
				(2, 999999, 'Seoul Forest', 12, 11, 0, 37.5444, 127.0374, 'Seoul Seongdong-gu', '2026-05-01T00:00:00Z', '2026-05-02T00:00:00Z')
			""");
		jdbcTemplate.update("""
			INSERT INTO tourism_source.attraction_images (
				id,
				attraction_no,
				source_type,
				public_url,
				display_order
			)
			VALUES (
				'00000000-0000-0000-0000-000000000011',
				1,
				'THUMBNAIL',
				'https://cdn.soomgil.example.com/places/126508.jpg',
				1
			)
			""");
	}

	@Test
	void findsOnlyPlacesInsideViewport() {
		List<PlaceViewportCandidate> candidates = handler.handle(new PlaceViewportCandidateQuery(
			"129.0,35.0,130.0,36.0",
			"ATTRACTION",
			10
		));

		assertThat(candidates).hasSize(1);
		PlaceViewportCandidate candidate = candidates.getFirst();
		assertThat(candidate.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(candidate.externalPlaceId()).isEqualTo("126508");
		assertThat(candidate.name()).isEqualTo("Haeundae Beach");
		assertThat(candidate.lat()).isEqualTo(35.1587);
		assertThat(candidate.lng()).isEqualTo(129.1604);
		assertThat(candidate.thumbnailUrl()).hasToString("https://cdn.soomgil.example.com/places/126508.jpg");
		assertThat(candidate.sourceStatus()).isEqualTo(PlaceSourceStatus.AVAILABLE);
	}

	@Test
	void returnsEmptyListWhenViewportIsInvalid() {
		List<PlaceViewportCandidate> candidates = handler.handle(new PlaceViewportCandidateQuery(
			"invalid",
			"ATTRACTION",
			10
		));

		assertThat(candidates).isEmpty();
	}
}
