package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceDetailRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TourismSourcePlaceDetailQueryHandlerTest {

	private RecordingTourismSourcePlaceDetailRepository repository;
	private TourismSourcePlaceDetailQueryHandler handler;

	@BeforeEach
	void setUp() {
		repository = new RecordingTourismSourcePlaceDetailRepository();
		handler = new TourismSourcePlaceDetailQueryHandler(repository);
	}

	@Test
	void findsTourismSourcePlaceDetailByProviderAndExternalPlaceId() {
		repository.item = new PlaceDetailItem(
			"126508",
			"Haeundae Beach",
			"Busan Haeundae-gu",
			35.1587,
			129.1604,
			URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
			"ATTRACTION",
			PlaceSourceStatus.AVAILABLE,
			"A representative Busan seaside attraction.",
			"+82-51-000-0000",
			OffsetDateTime.parse("2026-06-01T00:00:00Z"),
			true
		);

		PlaceDetail result = handler.handle(new PlaceDetailQuery(PlaceProvider.KTO, "126508"));

		assertThat(repository.lastQuery.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(repository.lastQuery.externalPlaceId()).isEqualTo("126508");

		assertThat(result.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(result.externalPlaceId()).isEqualTo("126508");
		assertThat(result.name()).isEqualTo("Haeundae Beach");
		assertThat(result.address()).isEqualTo("Busan Haeundae-gu");
		assertThat(result.lat()).isEqualTo(35.1587);
		assertThat(result.lng()).isEqualTo(129.1604);
		assertThat(result.thumbnailUrl()).isEqualTo(URI.create("https://cdn.soomgil.example.com/places/126508.jpg"));
		assertThat(result.category()).isEqualTo("ATTRACTION");
		assertThat(result.sourceStatus()).isEqualTo(PlaceSourceStatus.AVAILABLE);
		assertThat(result.description()).isEqualTo("A representative Busan seaside attraction.");
		assertThat(result.phone()).isEqualTo("+82-51-000-0000");
		assertThat(result.sourceUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
		assertThat(result.enriched()).isTrue();
	}

	private static final class RecordingTourismSourcePlaceDetailRepository extends TourismSourcePlaceDetailRepository {

		private PlaceDetailQuery lastQuery;
		private PlaceDetailItem item;

		@Override
		public PlaceDetailItem find(PlaceDetailQuery query) {
			lastQuery = query;
			return item;
		}
	}
}
