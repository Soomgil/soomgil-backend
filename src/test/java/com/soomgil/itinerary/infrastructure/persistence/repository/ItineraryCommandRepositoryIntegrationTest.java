package com.soomgil.itinerary.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ItineraryCommandRepositoryIntegrationTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000021");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000021");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000021");
	private static final UUID ORIGIN_ID = UUID.fromString("40000000-0000-0000-0000-000000000021");
	private static final UUID DESTINATION_ID = UUID.fromString("40000000-0000-0000-0000-000000000022");
	private static final UUID DRAWING_ID = UUID.fromString("50000000-0000-0000-0000-000000000021");
	private static final UUID ROUTE_ID = UUID.fromString("60000000-0000-0000-0000-000000000021");
	private static final Instant NOW = Instant.parse("2026-06-18T00:00:00Z");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ItineraryCommandRepository repository;

	@Test
	void persistsCoreItineraryResourcesAndVersion() {
		insertTrip();
		repository.insertDay(new ItineraryDayCreate(
			DAY_ID, TRIP_ID, ItineraryDayGroupType.DAY, 1, null, "첫째 날", 0, NOW, NOW
		));
		repository.insertItem(item(ORIGIN_ID, 0, "출발지"));
		repository.insertItem(item(DESTINATION_ID, 1, "도착지"));
		repository.insertMapDrawing(new MapDrawingCreate(
			DRAWING_ID,
			TRIP_ID,
			DAY_ID,
			DrawingType.LINE,
			GeometryFormat.GEOJSON,
			"{\"type\":\"LineString\",\"coordinates\":[[127.0,37.0],[127.1,37.1]]}",
			null,
			"산책로",
			0,
			0,
			USER_ID,
			USER_ID,
			NOW,
			NOW
		));
		repository.insertRouteSegment(new RouteSegmentCreate(
			ROUTE_ID,
			TRIP_ID,
			ORIGIN_ID,
			DESTINATION_ID,
			RouteMode.WALKING,
			"MAPBOX",
			"mapbox/walking",
			GeometryFormat.GEOJSON,
			"{\"type\":\"LineString\",\"coordinates\":[[127.0,37.0],[127.1,37.1]]}",
			100.0,
			60.0,
			0.9,
			USER_ID,
			USER_ID,
			NOW,
			NOW
		));

		assertThat(repository.findDay(TRIP_ID, DAY_ID)).isPresent();
		assertThat(repository.findItem(TRIP_ID, ORIGIN_ID).orElseThrow().placeName()).isEqualTo("출발지");
		assertThat(repository.findMapDrawing(TRIP_ID, DRAWING_ID)).isPresent();
		assertThat(repository.findRouteSegment(TRIP_ID, ROUTE_ID)).isPresent();
		assertThat(repository.incrementItineraryVersion(TRIP_ID, 0, NOW)).hasValue(1L);
		assertThat(repository.incrementItineraryVersion(TRIP_ID, 0, NOW)).isEmpty();
	}

	private ItineraryItemCreate item(UUID id, int sortOrder, String placeName) {
		return new ItineraryItemCreate(
			id,
			TRIP_ID,
			DAY_ID,
			sortOrder,
			ItineraryItemType.CUSTOM_PLACE,
			null,
			null,
			placeName,
			null,
			37.0,
			127.0,
			null,
			"AVAILABLE",
			USER_ID,
			USER_ID,
			NOW,
			NOW
		);
	}

	private void insertTrip() {
		OffsetDateTime now = OffsetDateTime.ofInstant(NOW, java.time.ZoneOffset.UTC);
		jdbcTemplate.update(
			"INSERT INTO trip.trips (id, owner_user_id, title, status, itinerary_version, created_at, updated_at) "
				+ "VALUES (?, ?, 'test', 'ACTIVE', 0, ?, ?)",
			TRIP_ID,
			USER_ID,
			now,
			now
		);
	}
}
