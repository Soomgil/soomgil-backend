package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.preference.application.command.dto.SavePlaceCommand;
import com.soomgil.preference.application.command.dto.UnsavePlaceCommand;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;
import com.soomgil.preference.application.query.handler.ListSavedPlacesQueryHandler;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import({
	TestcontainersConfiguration.class,
	SavedPlaceCommandHandlerIntegrationTest.CurrentUserTestConfiguration.class
})
@SpringBootTest
class SavedPlaceCommandHandlerIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");

	@Autowired
	private SavePlaceCommandHandler saveHandler;

	@Autowired
	private UnsavePlaceCommandHandler unsaveHandler;

	@Autowired
	private ListSavedPlacesQueryHandler listHandler;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM preference.user_saved_places");
		jdbcTemplate.update("DELETE FROM preference.user_swipe_events");
		jdbcTemplate.update("DELETE FROM preference.user_place_reactions");
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
				addr2
			)
			VALUES (1, 126508, 'Haeundae Beach', 12, 26, 2, 35.1587, 129.1604, 'Busan Haeundae-gu', 'Beach-ro')
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
				'00000000-0000-0000-0000-000000000802',
				1,
				'THUMBNAIL',
				'https://cdn.soomgil.example.com/places/126508.jpg',
				1
			)
			""");
	}

	@Test
	void savesListsAndUnsavesSuperLikedPlace() {
		insertReaction("SUPER_LIKE");

		SavedPlace savedPlace = saveHandler.handle(new SavePlaceCommand(PlaceProvider.KTO, "126508"));

		assertThat(savedPlace.place().externalPlaceId()).isEqualTo("126508");
		assertThat(savedPlace.place().name()).isEqualTo("Haeundae Beach");
		assertThat(savedPlace.place().thumbnailUrl()).hasToString("https://cdn.soomgil.example.com/places/126508.jpg");

		PagedSavedPlace savedPlaces = listHandler.handle(new ListSavedPlacesQuery(0, 20));
		assertThat(savedPlaces.items()).hasSize(1);
		assertThat(savedPlaces.items().getFirst().id()).isEqualTo(savedPlace.id());
		assertThat(savedPlaces.page().totalElements()).isEqualTo(1L);

		unsaveHandler.handle(new UnsavePlaceCommand(PlaceProvider.KTO, "126508"));

		PagedSavedPlace afterUnsave = listHandler.handle(new ListSavedPlacesQuery(0, 20));
		assertThat(afterUnsave.items()).isEmpty();
		assertThat(afterUnsave.page().totalElements()).isZero();
	}

	@Test
	void refusesToSavePlaceWithoutSuperLikeReaction() {
		insertReaction("LIKE");

		assertThatThrownBy(() -> saveHandler.handle(new SavePlaceCommand(PlaceProvider.KTO, "126508")))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	private void insertReaction(String reaction) {
		jdbcTemplate.update("""
			INSERT INTO preference.user_place_reactions (
				id,
				user_id,
				provider,
				external_place_id,
				reaction
			)
			VALUES ('00000000-0000-0000-0000-000000000803', ?, 'KTO', '126508', ?)
			""",
			USER_ID,
			reaction
		);
	}

	@TestConfiguration
	static class CurrentUserTestConfiguration {

		@Bean
		CurrentUserProvider currentUserProvider() {
			return () -> new CurrentUser(USER_ID, "min@example.com");
		}
	}
}
