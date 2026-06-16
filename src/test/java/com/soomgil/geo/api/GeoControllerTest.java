package com.soomgil.geo.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.geo.api.dto.LegalRegionLevel;
import com.soomgil.geo.api.dto.PagedLegalRegion;
import com.soomgil.geo.application.port.LegalRegionPage;
import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.port.LegalRegionReadModel;
import com.soomgil.geo.application.query.handler.ListLegalRegionsHandler;
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
		GeoController controller = new GeoController(new ListLegalRegionsHandler(repository));

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
