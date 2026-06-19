package com.soomgil.place.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.application.query.dto.PlaceSearchResult;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourcePlaceSearchMapper;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceSearchRow;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TourismSourcePlaceSearchRepositoryTest {

	private RecordingTourismSourcePlaceSearchMapper mapper;
	private TourismSourcePlaceSearchRepository repository;

	@BeforeEach
	void setUp() {
		mapper = new RecordingTourismSourcePlaceSearchMapper();
		repository = new TourismSourcePlaceSearchRepository(mapper);
	}

	@Test
	void mapsTourismSourceRowsToSearchResult() {
		mapper.count = 1;
		mapper.rows = List.of(new TourismSourcePlaceSearchRow(
			126508,
			"Haeundae Beach",
			"Busan Haeundae-gu",
			35.1587,
			129.1604,
			URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
			"ATTRACTION"
		));

		PlaceSearchResult result = repository.search(new PlaceSearchCriteria(
			"Busan beach",
			"129.0,35.0,130.0,36.0",
			"26000",
			"ATTRACTION",
			0,
			20
		));

		assertThat(mapper.lastCountCriteria.q()).isEqualTo("Busan beach");
		assertThat(mapper.lastSearchCriteria.bbox()).isEqualTo("129.0,35.0,130.0,36.0");

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().externalPlaceId()).isEqualTo("126508");
		assertThat(result.items().getFirst().name()).isEqualTo("Haeundae Beach");
		assertThat(result.items().getFirst().sourceStatus()).isEqualTo(PlaceSourceStatus.AVAILABLE);
		assertThat(result.page().page()).isZero();
		assertThat(result.page().size()).isEqualTo(20);
		assertThat(result.page().totalElements()).isEqualTo(1L);
		assertThat(result.page().totalPages()).isEqualTo(1);
	}

	@Test
	void returnsEmptyPageWhenMapperFindsNoRows() {
		mapper.count = 0;
		mapper.rows = List.of();

		PlaceSearchResult result = repository.search(new PlaceSearchCriteria(
			"unknown",
			null,
			null,
			null,
			1,
			20
		));

		assertThat(result.items()).isEmpty();
		assertThat(result.page().page()).isEqualTo(1);
		assertThat(result.page().totalElements()).isZero();
		assertThat(result.page().totalPages()).isZero();
	}

	private static final class RecordingTourismSourcePlaceSearchMapper implements TourismSourcePlaceSearchMapper {

		private PlaceSearchCriteria lastCountCriteria;
		private PlaceSearchCriteria lastSearchCriteria;
		private long count;
		private List<TourismSourcePlaceSearchRow> rows = List.of();

		@Override
		public long count(PlaceSearchCriteria criteria) {
			lastCountCriteria = criteria;
			return count;
		}

		@Override
		public List<TourismSourcePlaceSearchRow> search(PlaceSearchCriteria criteria) {
			lastSearchCriteria = criteria;
			return rows;
		}
	}
}
