package com.soomgil.place.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soomgil.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PlaceControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

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
				tel,
				addr1,
				addr2,
				overview,
				source_modified_at,
				imported_at
			)
			VALUES (
				1,
				126508,
				'Haeundae Beach',
				12,
				26,
				0,
				35.1587,
				129.1604,
				'+82-51-000-0000',
				'Busan Haeundae-gu',
				'Beach-ro',
				'A representative Busan seaside attraction.',
				'2026-06-01T00:00:00Z'::timestamptz,
				'2026-06-02T00:00:00Z'::timestamptz
			)
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
				'00000000-0000-0000-0000-000000000001',
				1,
				'THUMBNAIL',
				'https://cdn.soomgil.example.com/places/126508.jpg',
				1
			)
			""");
	}

	@Test
	void searchPlacesReadsTourismSourceData() throws Exception {
		mockMvc.perform(get("/api/v1/places/search")
				.param("q", "Beach")
				.param("bbox", "129.0,35.0,130.0,36.0")
				.param("legalRegionCode", "26000")
				.param("category", "ATTRACTION")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].provider").value("KTO"))
			.andExpect(jsonPath("$.items[0].externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.items[0].name").value("Haeundae Beach"))
			.andExpect(jsonPath("$.items[0].address").value("Busan Haeundae-gu Beach-ro"))
			.andExpect(jsonPath("$.items[0].thumbnailUrl").value("https://cdn.soomgil.example.com/places/126508.jpg"))
			.andExpect(jsonPath("$.items[0].sourceStatus").value("AVAILABLE"))
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	@Test
	void getPlaceReadsTourismSourceDetailData() throws Exception {
		mockMvc.perform(get("/api/v1/places/KTO/126508"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.provider").value("KTO"))
			.andExpect(jsonPath("$.externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.name").value("Haeundae Beach"))
			.andExpect(jsonPath("$.address").value("Busan Haeundae-gu Beach-ro"))
			.andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.soomgil.example.com/places/126508.jpg"))
			.andExpect(jsonPath("$.sourceStatus").value("AVAILABLE"))
			.andExpect(jsonPath("$.description").value("A representative Busan seaside attraction."))
			.andExpect(jsonPath("$.phone").value("+82-51-000-0000"))
			.andExpect(jsonPath("$.sourceUpdatedAt").value("2026-06-01T00:00:00Z"))
			.andExpect(jsonPath("$.enriched").value(false));
	}

	@Test
	void getPlaceReturnsNotFoundWhenTourismSourcePlaceDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/v1/places/KTO/missing"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}
}
