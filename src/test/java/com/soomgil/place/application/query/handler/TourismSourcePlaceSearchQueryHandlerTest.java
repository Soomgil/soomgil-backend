package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.application.query.dto.PlaceSearchItem;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.dto.PlaceSearchResult;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceSearchRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TourismSourcePlaceSearchQueryHandlerTest {

	private RecordingTourismSourcePlaceSearchRepository repository;
	private TourismSourcePlaceSearchQueryHandler handler;

	@BeforeEach
	void setUp() {
		repository = new RecordingTourismSourcePlaceSearchRepository();
		handler = new TourismSourcePlaceSearchQueryHandler(repository);
	}

	@Test
	void searchesTourismSourcePlacesWithQueryCriteria() {
		repository.result = new PlaceSearchResult(
			List.of(new PlaceSearchItem(
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

		PagedPlaceSummary result = handler.handle(new PlaceSearchQuery(
			"Busan beach",
			"129.0,35.0,130.0,36.0",
			"26000",
			"ATTRACTION",
			0,
			20
		));

		assertThat(repository.lastCriteria.q()).isEqualTo("Busan beach");
		assertThat(repository.lastCriteria.bbox()).isEqualTo("129.0,35.0,130.0,36.0");
		assertThat(repository.lastCriteria.legalRegionCode()).isEqualTo("26000");
		assertThat(repository.lastCriteria.category()).isEqualTo("ATTRACTION");
		assertThat(repository.lastCriteria.page()).isZero();
		assertThat(repository.lastCriteria.size()).isEqualTo(20);

		assertThat(result.items()).hasSize(1);
		PlaceSummary item = result.items().getFirst();
		assertThat(item.provider()).isEqualTo(PlaceProvider.KTO);
		assertThat(item.externalPlaceId()).isEqualTo("126508");
		assertThat(item.name()).isEqualTo("Haeundae Beach");
		assertThat(item.address()).isEqualTo("Busan Haeundae-gu");
		assertThat(item.lat()).isEqualTo(35.1587);
		assertThat(item.lng()).isEqualTo(129.1604);
		assertThat(item.thumbnailUrl()).isEqualTo(URI.create("https://cdn.soomgil.example.com/places/126508.jpg"));
		assertThat(item.category()).isEqualTo("ATTRACTION");
		assertThat(item.sourceStatus()).isEqualTo(PlaceSourceStatus.AVAILABLE);
		assertThat(result.page().totalElements()).isEqualTo(1L);
	}

	@Test
	void returnsEmptyPageWhenRepositoryFindsNoPlaces() {
		repository.result = new PlaceSearchResult(List.of(), new PageMeta(1, 20, 0L, 0, List.of()));

		PagedPlaceSummary result = handler.handle(new PlaceSearchQuery("unknown", null, null, null, 1, 20));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().page()).isEqualTo(1);
		assertThat(result.page().totalElements()).isZero();
	}

	private static final class RecordingTourismSourcePlaceSearchRepository extends TourismSourcePlaceSearchRepository {

		private PlaceSearchCriteria lastCriteria;
		private PlaceSearchResult result;

		@Override
		public PlaceSearchResult search(PlaceSearchCriteria criteria) {
			lastCriteria = criteria;
			return result;
		}
	}
}
