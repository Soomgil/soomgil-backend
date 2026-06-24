package com.soomgil.place.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.handler.PlaceDetailQueryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.place.application.query.handler.PopularPlacesQueryHandler;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlaceControllerTest {

	private RecordingPlaceSearchQueryHandler searchHandler;
	private RecordingPlaceDetailQueryHandler detailHandler;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		searchHandler = new RecordingPlaceSearchQueryHandler();
		detailHandler = new RecordingPlaceDetailQueryHandler();
		mockMvc = MockMvcBuilders.standaloneSetup(new PlaceController(
			searchHandler, detailHandler, mock(PopularPlacesQueryHandler.class)
		))
			.setControllerAdvice(new GlobalExceptionHandler(new ProblemDetailsFactory()))
			.setMessageConverters(new MappingJackson2HttpMessageConverter(
				Jackson2ObjectMapperBuilder.json()
					.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
					.build()
			))
			.build();
	}

	@Test
	void searchPlacesDelegatesToQueryHandler() throws Exception {
		mockMvc.perform(get("/api/v1/places/search")
				.param("q", "Busan beach")
				.param("bbox", "129.0,35.0,130.0,36.0")
				.param("legalRegionCode", "26000")
				.param("category", "ATTRACTION")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].provider").value("KTO"))
			.andExpect(jsonPath("$.items[0].externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.items[0].name").value("Haeundae Beach"))
			.andExpect(jsonPath("$.items[0].sourceStatus").value("AVAILABLE"))
			.andExpect(jsonPath("$.page.page").value(0))
			.andExpect(jsonPath("$.page.size").value(20))
			.andExpect(jsonPath("$.page.totalElements").value(1));

		assertThat(searchHandler.lastQuery.q()).isEqualTo("Busan beach");
		assertThat(searchHandler.lastQuery.bbox()).isEqualTo("129.0,35.0,130.0,36.0");
		assertThat(searchHandler.lastQuery.legalRegionCode()).isEqualTo("26000");
		assertThat(searchHandler.lastQuery.category()).isEqualTo("ATTRACTION");
		assertThat(searchHandler.lastQuery.page()).isZero();
		assertThat(searchHandler.lastQuery.size()).isEqualTo(20);
	}

	@Test
	void getPlaceDelegatesToQueryHandler() throws Exception {
		mockMvc.perform(get("/api/v1/places/KTO/126508"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.provider").value("KTO"))
			.andExpect(jsonPath("$.externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.name").value("Haeundae Beach"))
			.andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.soomgil.example.com/places/126508.jpg"))
			.andExpect(jsonPath("$.sourceStatus").value("AVAILABLE"))
			.andExpect(jsonPath("$.description").value("A representative Busan seaside attraction."))
			.andExpect(jsonPath("$.phone").value("+82-51-000-0000"))
			.andExpect(jsonPath("$.sourceUpdatedAt").value("2026-06-01T00:00:00Z"))
			.andExpect(jsonPath("$.enriched").value(true));

		assertThat(detailHandler.lastQuery.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(detailHandler.lastQuery.externalPlaceId()).isEqualTo("126508");
	}

	@Test
	void getPlaceReturnsProblemDetailsWhenPlaceDoesNotExist() throws Exception {
		detailHandler.failure = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Place was not found.");

		mockMvc.perform(get("/api/v1/places/KTO/missing"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.method").value("GET"))
			.andExpect(jsonPath("$.instance").value("/api/v1/places/KTO/missing"));
	}

	private static final class RecordingPlaceSearchQueryHandler implements PlaceSearchQueryHandler {

		private PlaceSearchQuery lastQuery;

		@Override
		public PagedPlaceSummary handle(PlaceSearchQuery query) {
			lastQuery = query;
			return new PagedPlaceSummary(
				List.of(new PlaceSummary(
					PlaceProvider.KTO,
					"126508",
					"Haeundae Beach",
					"Busan Haeundae-gu",
					35.1587,
					129.1604,
					URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
					"ATTRACTION",
					PlaceSourceStatus.AVAILABLE
				)),
				new PageMeta(0, 20, 1L, 1, List.of())
			);
		}
	}

	private static final class RecordingPlaceDetailQueryHandler implements PlaceDetailQueryHandler {

		private PlaceDetailQuery lastQuery;
		private RuntimeException failure;

		@Override
		public PlaceDetail handle(PlaceDetailQuery query) {
			lastQuery = query;
			if (failure != null) {
				throw failure;
			}
			return new PlaceDetail(
				PlaceProvider.KTO,
				"126508",
				"Haeundae Beach",
				"Busan Haeundae-gu",
				35.1587,
				129.1604,
				URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
				List.of(URI.create("https://cdn.soomgil.example.com/places/126508.jpg")),
				"ATTRACTION",
				PlaceSourceStatus.AVAILABLE,
				"A representative Busan seaside attraction.",
				"+82-51-000-0000",
				OffsetDateTime.parse("2026-06-01T00:00:00Z"),
				true
			);
		}
	}
}
