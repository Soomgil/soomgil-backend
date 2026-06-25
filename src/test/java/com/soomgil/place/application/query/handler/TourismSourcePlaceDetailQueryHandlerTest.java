package com.soomgil.place.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.application.query.dto.AccessibilityFlag;
import com.soomgil.place.application.query.dto.ParkingType;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourcePlaceDetailMapper;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourcePlaceImageMapper;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceDetailRepository;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceImageRepository;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceDetailRow;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceImageRow;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TourismSourcePlaceDetailQueryHandlerTest {

	private RecordingTourismSourcePlaceDetailRepository repository;
	private RecordingPlaceAccessibilityCacheService accessibilityCacheService;
	private TourismSourcePlaceDetailQueryHandler handler;

	@BeforeEach
	void setUp() {
		repository = new RecordingTourismSourcePlaceDetailRepository();
		accessibilityCacheService = new RecordingPlaceAccessibilityCacheService();
		handler = new TourismSourcePlaceDetailQueryHandler(repository, accessibilityCacheService);
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
			List.of(
				URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
				URI.create("https://cdn.soomgil.example.com/places/126508-2.jpg")
			),
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
		assertThat(result.photos()).containsExactly(
			URI.create("https://cdn.soomgil.example.com/places/126508.jpg"),
			URI.create("https://cdn.soomgil.example.com/places/126508-2.jpg")
		);
		assertThat(result.category()).isEqualTo("ATTRACTION");
		assertThat(result.sourceStatus()).isEqualTo(PlaceSourceStatus.AVAILABLE);
		assertThat(result.description()).isEqualTo("A representative Busan seaside attraction.");
		assertThat(result.phone()).isEqualTo("+82-51-000-0000");
		assertThat(result.sourceUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
		assertThat(result.enriched()).isTrue();
		assertThat(accessibilityCacheService.lastRefs).containsExactly(new PlaceAccessibilityCacheService.PlaceRef(
			"KTO",
			"126508",
			null
		));
		assertThat(result.accessibility().flags()).containsExactly(AccessibilityFlag.WHEELCHAIR);
	}

	private static final class RecordingPlaceAccessibilityCacheService extends PlaceAccessibilityCacheService {

		private List<PlaceRef> lastRefs = List.of();

		private RecordingPlaceAccessibilityCacheService() {
			super(null);
		}

		@Override
		public Map<String, PlaceAccessibilityInfo> getMany(List<PlaceRef> refs) {
			lastRefs = refs;
			return Map.of(
				"KTO:126508",
				new PlaceAccessibilityInfo(null, null, ParkingType.FREE, Set.of(AccessibilityFlag.WHEELCHAIR), Set.of())
			);
		}
	}

	private static final class RecordingTourismSourcePlaceDetailRepository extends TourismSourcePlaceDetailRepository {

		private PlaceDetailQuery lastQuery;
		private PlaceDetailItem item;

		private RecordingTourismSourcePlaceDetailRepository() {
			super(
				new NoopTourismSourcePlaceDetailMapper(),
				new TourismSourcePlaceImageRepository(new NoopTourismSourcePlaceImageMapper())
			);
		}

		@Override
		public PlaceDetailItem find(PlaceDetailQuery query) {
			lastQuery = query;
			return item;
		}
	}

	private static final class NoopTourismSourcePlaceDetailMapper implements TourismSourcePlaceDetailMapper {

		@Override
		public TourismSourcePlaceDetailRow findByContentId(String contentId) {
			return null;
		}
	}

	private static final class NoopTourismSourcePlaceImageMapper implements TourismSourcePlaceImageMapper {

		@Override
		public List<TourismSourcePlaceImageRow> findNormalImages(String contentId) {
			return List.of();
		}

		@Override
		public TourismSourcePlaceImageRow findAwardImage(String contentId) {
			return null;
		}
	}
}
