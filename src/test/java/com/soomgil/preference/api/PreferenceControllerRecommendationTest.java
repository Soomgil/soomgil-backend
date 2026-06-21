package com.soomgil.preference.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.api.dto.PlaceRecommendation;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import com.soomgil.user.api.dto.UserSummary;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PreferenceControllerRecommendationTest {

	private RecordingListPlaceRecommendationsQueryHandler recommendationHandler;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		recommendationHandler = new RecordingListPlaceRecommendationsQueryHandler();
		mockMvc = MockMvcBuilders.standaloneSetup(new PreferenceController(recommendationHandler)).build();
	}

	@Test
	void listPlaceRecommendationsDelegatesViewportAndTabToQueryHandler() throws Exception {
		UUID tripId = UUID.fromString("00000000-0000-0000-0000-000000002001");

		mockMvc.perform(get("/api/v1/trips/{tripId}/place-recommendations", tripId)
				.param("bbox", "129.0,35.0,130.0,36.0")
				.param("centerLat", "35.5")
				.param("centerLng", "129.5")
				.param("tab", "BASIC")
				.param("page", "1")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].place.externalPlaceId").value("cafe"))
			.andExpect(jsonPath("$.items[0].matchedMembers[0].displayName").value("여행 멤버"))
			.andExpect(jsonPath("$.items[0].score").doesNotExist())
			.andExpect(jsonPath("$.page.page").value(1));

		assertThat(recommendationHandler.lastQuery.tripId()).isEqualTo(tripId);
		assertThat(recommendationHandler.lastQuery.bbox()).isEqualTo("129.0,35.0,130.0,36.0");
		assertThat(recommendationHandler.lastQuery.centerLat()).isEqualTo(35.5);
		assertThat(recommendationHandler.lastQuery.centerLng()).isEqualTo(129.5);
		assertThat(recommendationHandler.lastQuery.tab()).isEqualTo(RecommendationTab.BASIC);
		assertThat(recommendationHandler.lastQuery.page()).isEqualTo(1);
		assertThat(recommendationHandler.lastQuery.size()).isEqualTo(10);
	}

	@Test
	void listPlaceRecommendationsRequiresViewportBounds() throws Exception {
		mockMvc.perform(get(
				"/api/v1/trips/{tripId}/place-recommendations",
				UUID.fromString("00000000-0000-0000-0000-000000002001")
			))
			.andExpect(status().isBadRequest());
	}

	private static final class RecordingListPlaceRecommendationsQueryHandler
		implements ListPlaceRecommendationsQueryHandler {

		private ListPlaceRecommendationsQuery lastQuery;

		@Override
		public PagedPlaceRecommendation handle(ListPlaceRecommendationsQuery query) {
			lastQuery = query;
			return new PagedPlaceRecommendation(
				List.of(new PlaceRecommendation(
					new PlaceSummary(
						PlaceProvider.KTO,
						"cafe",
						"Quiet Cafe",
						"Busan",
						35.5,
						129.5,
						null,
						"ATTRACTION",
						PlaceSourceStatus.AVAILABLE
					),
					List.of(new UserSummary(
						UUID.fromString("00000000-0000-0000-0000-000000002003"),
						"여행 멤버",
						null
					)),
					1,
					0.0,
					"여행 멤버의 취향과 잘 맞아요"
				)),
				new PageMeta(query.page(), query.size(), 1L, 1, List.of())
			);
		}
	}
}
