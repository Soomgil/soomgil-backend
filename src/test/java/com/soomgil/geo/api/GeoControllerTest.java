package com.soomgil.geo.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.soomgil.geo.api.dto.LegalRegionLevel;
import com.soomgil.geo.api.dto.LngLat;
import com.soomgil.geo.api.dto.PagedLegalRegion;
import com.soomgil.geo.api.dto.SimplifiedCoordinates;
import com.soomgil.geo.api.dto.SimplifyCoordinatesRequest;
import com.soomgil.geo.api.dto.ViewportSummary;
import com.soomgil.geo.application.port.LegalRegionPage;
import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.port.LegalRegionReadModel;
import com.soomgil.geo.application.query.handler.GeoCoordinateHandler;
import com.soomgil.geo.application.query.handler.ListLegalRegionsHandler;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeoControllerTest {

	@Test
	void listLegalRegionsReturnsPagedResponse() {
		StubLegalRegionQueryRepository repository = new StubLegalRegionQueryRepository();
		repository.result = new LegalRegionPage(List.of(new LegalRegionReadModel(
			"1100000000",
			"서울특별시",
			"서울특별시",
			com.soomgil.geo.domain.model.LegalRegionLevel.SIDO,
			null,
			true
		)), 1);
		GeoController controller = new GeoController(new ListLegalRegionsHandler(repository), new GeoCoordinateHandler());

		PagedLegalRegion result = controller.listLegalRegions(
			"서울",
			LegalRegionLevel.SIDO,
			null,
			true,
			0,
			20,
			List.of("fullName,asc")
		);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).code()).isEqualTo("1100000000");
		assertThat(result.items().get(0).level()).isEqualTo(LegalRegionLevel.SIDO);
		assertThat(result.page().totalElements()).isEqualTo(1);
	}

	@Test
	void summarizesViewport() {
		GeoController controller = controller();

		ViewportSummary result = controller.summarizeViewport("126.7,37.4,127.2,37.8");

		assertThat(result.viewport().minLng()).isEqualTo(126.7);
		assertThat(result.center().lng()).isCloseTo(126.95, within(0.000001));
		assertThat(result.center().lat()).isCloseTo(37.6, within(0.000001));
		assertThat(result.widthMeters()).isPositive();
		assertThat(result.heightMeters()).isPositive();
	}

	@Test
	void rejectsInvalidViewport() {
		GeoController controller = controller();

		assertThatThrownBy(() -> controller.summarizeViewport("127,37,126,38"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
			);
	}

	@Test
	void simplifiesCoordinatesKeepingEndpoints() {
		GeoController controller = controller();
		List<LngLat> coordinates = new ArrayList<>();
		for (int index = 0; index < 120; index++) {
			coordinates.add(new LngLat(127.0 + index * 0.001, 37.0 + index * 0.001));
		}

		SimplifiedCoordinates result = controller.simplifyCoordinates(new SimplifyCoordinatesRequest(coordinates, 100));

		assertThat(result.originalCount()).isEqualTo(120);
		assertThat(result.simplifiedCount()).isEqualTo(100);
		assertThat(result.coordinates().get(0)).isEqualTo(coordinates.get(0));
		assertThat(result.coordinates().get(result.coordinates().size() - 1)).isEqualTo(coordinates.get(coordinates.size() - 1));
	}

	@Test
	void rejectsCoordinateOutOfRange() {
		GeoController controller = controller();

		assertThatThrownBy(() -> controller.simplifyCoordinates(new SimplifyCoordinatesRequest(
			List.of(new LngLat(127.0, 37.0), new LngLat(181.0, 37.0)),
			100
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	private GeoController controller() {
		return new GeoController(
			new ListLegalRegionsHandler(new StubLegalRegionQueryRepository()),
			new GeoCoordinateHandler()
		);
	}

	private static class StubLegalRegionQueryRepository implements LegalRegionQueryRepository {

		private LegalRegionPage result = new LegalRegionPage(List.of(), 0);

		@Override
		public LegalRegionPage findLegalRegions(
			String query,
			com.soomgil.geo.domain.model.LegalRegionLevel level,
			String parentCode,
			Boolean isActive,
			int page,
			int size
		) {
			return result;
		}
	}
}
