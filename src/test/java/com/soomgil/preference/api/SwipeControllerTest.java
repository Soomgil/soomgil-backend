package com.soomgil.preference.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.api.dto.MyPreferenceSummary;
import com.soomgil.preference.api.dto.MyPreferenceCategory;
import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.preference.api.dto.SwipeFeedItem;
import com.soomgil.preference.api.dto.SwipeFeedPlace;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.api.dto.SwipeReactionRequest;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.application.command.dto.SavePlaceCommand;
import com.soomgil.preference.application.command.dto.UnsavePlaceCommand;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import com.soomgil.preference.application.command.handler.SavePlaceCommandHandler;
import com.soomgil.preference.application.command.handler.UnsavePlaceCommandHandler;
import com.soomgil.preference.application.command.handler.UpsertSwipeReactionCommandHandler;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;
import com.soomgil.preference.application.query.dto.ListMyPreferencesQuery;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.application.query.handler.ListSavedPlacesQueryHandler;
import com.soomgil.preference.application.query.handler.ListMyPreferencesQueryHandler;
import com.soomgil.preference.application.query.handler.SwipeFeedQueryHandler;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.soomgil.preference.application.service.SwipeTagPreparationService;
import com.soomgil.preference.api.dto.SwipeTagStatus;
import com.soomgil.preference.api.dto.TagPreparationStatus;

class SwipeControllerTest {

	private RecordingSwipeFeedQueryHandler feedHandler;
	private RecordingUpsertSwipeReactionCommandHandler reactionHandler;
	private RecordingSavePlaceCommandHandler saveHandler;
	private RecordingUnsavePlaceCommandHandler unsaveHandler;
	private RecordingListSavedPlacesQueryHandler listSavedPlacesHandler;
	private RecordingListMyPreferencesQueryHandler listMyPreferencesHandler;
	private ObjectMapper objectMapper;
	private MockMvc mockMvc;
	private SwipeTagPreparationService tagPreparationService;

	@BeforeEach
	void setUp() {
		feedHandler = new RecordingSwipeFeedQueryHandler();
		reactionHandler = new RecordingUpsertSwipeReactionCommandHandler();
		saveHandler = new RecordingSavePlaceCommandHandler();
		unsaveHandler = new RecordingUnsavePlaceCommandHandler();
		listSavedPlacesHandler = new RecordingListSavedPlacesQueryHandler();
		listMyPreferencesHandler = new RecordingListMyPreferencesQueryHandler();
		objectMapper = Jackson2ObjectMapperBuilder.json()
			.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();
		tagPreparationService = org.mockito.Mockito.mock(SwipeTagPreparationService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new SwipeController(
				feedHandler,
				reactionHandler,
				saveHandler,
				unsaveHandler,
				listSavedPlacesHandler,
				listMyPreferencesHandler,
				tagPreparationService
			))
			.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
			.build();
	}

	@Test
	void getSwipeFeedDelegatesToQueryHandler() throws Exception {
		mockMvc.perform(get("/api/v1/swipe/feed")
				.param("legalRegionCode", "26")
				.param("category", "ATTRACTION")
				.param("limit", "10")
				.param("excludeRecent", "true")
				.param("seed", "abc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].place.externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.items[0].myReaction").value("LIKE"));

		assertThat(feedHandler.lastQuery.legalRegionCode()).isEqualTo("26");
		assertThat(feedHandler.lastQuery.category()).isEqualTo("ATTRACTION");
		assertThat(feedHandler.lastQuery.limit()).isEqualTo(10);
		assertThat(feedHandler.lastQuery.excludeRecent()).isTrue();
		assertThat(feedHandler.lastQuery.seed()).isEqualTo("abc");
	}

	@Test
	void getsTagPreparationStatusesForBufferedCards() throws Exception {
		org.mockito.Mockito.when(tagPreparationService.findStatuses(List.of("126508", "999999")))
			.thenReturn(List.of(
				new SwipeTagStatus("126508", List.of("바다"), TagPreparationStatus.READY),
				new SwipeTagStatus("999999", List.of(), TagPreparationStatus.PENDING)
			));

		mockMvc.perform(get("/api/v1/swipe/tags")
				.param("externalPlaceIds", "126508,999999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].status").value("READY"))
			.andExpect(jsonPath("$[1].status").value("PENDING"));
	}

	@Test
	void upsertSwipeReactionDelegatesToCommandHandler() throws Exception {
		mockMvc.perform(put("/api/v1/places/KTO/126508/swipe-reaction")
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(new SwipeReactionRequest(SwipeReaction.SUPER_LIKE, "feed"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.place.externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.reaction").value("SUPER_LIKE"))
			.andExpect(jsonPath("$.savedPlaceEligible").value(true));

		assertThat(reactionHandler.lastCommand.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(reactionHandler.lastCommand.externalPlaceId()).isEqualTo("126508");
		assertThat(reactionHandler.lastCommand.reaction()).isEqualTo(SwipeReaction.SUPER_LIKE);
	}

	@Test
	void listSavedPlacesDelegatesToQueryHandler() throws Exception {
		mockMvc.perform(get("/api/v1/me/saved-places")
				.param("page", "1")
				.param("size", "5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].place.externalPlaceId").value("126508"))
			.andExpect(jsonPath("$.page.page").value(1));

		assertThat(listSavedPlacesHandler.lastQuery.page()).isEqualTo(1);
		assertThat(listSavedPlacesHandler.lastQuery.size()).isEqualTo(5);
	}

	@Test
	void listMyPreferencesDelegatesToQueryHandler() throws Exception {
		mockMvc.perform(get("/api/v1/me/preferences"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.topCategories[0].category").value("자연/경관"))
			.andExpect(jsonPath("$.topCategories[0].groupCode").value("nature_scene"))
			.andExpect(jsonPath("$.topCategories[0].percentage").value(78))
			.andExpect(jsonPath("$.travelStyle").value("자연/경관와 역사/문화을(를) 함께 즐기는 여행을 선호해요."))
			.andExpect(jsonPath("$.preferredTags[0]").value("자연"));

		assertThat(listMyPreferencesHandler.invoked).isTrue();
	}

	@Test
	void savePlaceDelegatesToCommandHandler() throws Exception {
		mockMvc.perform(put("/api/v1/places/KTO/126508/save"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.place.externalPlaceId").value("126508"));

		assertThat(saveHandler.lastCommand.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(saveHandler.lastCommand.externalPlaceId()).isEqualTo("126508");
	}

	@Test
	void unsavePlaceDelegatesToCommandHandler() throws Exception {
		mockMvc.perform(delete("/api/v1/places/KTO/126508/save"))
			.andExpect(status().isNoContent());

		assertThat(unsaveHandler.lastCommand.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(unsaveHandler.lastCommand.externalPlaceId()).isEqualTo("126508");
	}

	private static PlaceSummary place() {
		return new PlaceSummary(
			PlaceProvider.KTO,
			"126508",
			"Haeundae Beach",
			"Busan Haeundae-gu",
			35.1587,
			129.1604,
			URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
			"ATTRACTION",
			PlaceSourceStatus.AVAILABLE
		);
	}

	private static SavedPlace savedPlace() {
		return new SavedPlace(
			UUID.fromString("00000000-0000-0000-0000-000000001001"),
			place(),
			OffsetDateTime.parse("2026-06-01T00:00:00Z")
		);
	}

	private static SwipeFeedPlace swipePlace() {
		return new SwipeFeedPlace(
			PlaceProvider.KTO,
			"126508",
			"Haeundae Beach",
			"Busan Haeundae-gu",
			35.1587,
			129.1604,
			"https://cdn.soomgil.example.com/places/126508.jpg",
			"관광지",
			PlaceSourceStatus.AVAILABLE,
			"Beach",
			List.of("https://cdn.soomgil.example.com/places/126508.jpg"),
			List.of("바다·해안")
		);
	}

	private static final class RecordingSwipeFeedQueryHandler implements SwipeFeedQueryHandler {

		private SwipeFeedQuery lastQuery;

		@Override
		public SwipeFeedResponse handle(SwipeFeedQuery query) {
			lastQuery = query;
			return new SwipeFeedResponse(List.of(new SwipeFeedItem(swipePlace(), SwipeReaction.LIKE, List.of())), null);
		}
	}

	private static final class RecordingUpsertSwipeReactionCommandHandler
		implements UpsertSwipeReactionCommandHandler {

		private UpsertSwipeReactionCommand lastCommand;

		@Override
		public SwipeReactionResponse handle(UpsertSwipeReactionCommand command) {
			lastCommand = command;
			return new SwipeReactionResponse(
				new PlaceRef(command.provider(), command.externalPlaceId()),
				command.reaction(),
				true,
				OffsetDateTime.now()
			);
		}
	}

	private static final class RecordingSavePlaceCommandHandler implements SavePlaceCommandHandler {

		private SavePlaceCommand lastCommand;

		@Override
		public SavedPlace handle(SavePlaceCommand command) {
			lastCommand = command;
			return savedPlace();
		}
	}

	private static final class RecordingUnsavePlaceCommandHandler implements UnsavePlaceCommandHandler {

		private UnsavePlaceCommand lastCommand;

		@Override
		public NoResult handle(UnsavePlaceCommand command) {
			lastCommand = command;
			return NoResult.INSTANCE;
		}
	}

	private static final class RecordingListSavedPlacesQueryHandler implements ListSavedPlacesQueryHandler {

		private ListSavedPlacesQuery lastQuery;

		@Override
		public PagedSavedPlace handle(ListSavedPlacesQuery query) {
			lastQuery = query;
			return new PagedSavedPlace(List.of(savedPlace()), new PageMeta(query.page(), query.size(), 1L, 1, List.of()));
		}
	}

	private static final class RecordingListMyPreferencesQueryHandler implements ListMyPreferencesQueryHandler {

		private boolean invoked;

		@Override
		public MyPreferenceSummary handle(ListMyPreferencesQuery query) {
			invoked = true;
			return new MyPreferenceSummary(
				List.of(
					new MyPreferenceCategory("자연/경관", "nature_scene", 78),
					new MyPreferenceCategory("역사/문화", "history_culture", 65)
				),
				"자연/경관와 역사/문화을(를) 함께 즐기는 여행을 선호해요.",
				List.of("자연", "산책", "역사", "전통건축")
			);
		}
	}
}
