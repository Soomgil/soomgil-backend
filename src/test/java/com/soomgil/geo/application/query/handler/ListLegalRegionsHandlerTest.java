package com.soomgil.geo.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.geo.application.port.LegalRegionPage;
import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.port.LegalRegionReadModel;
import com.soomgil.geo.application.query.dto.ListLegalRegionsQuery;
import com.soomgil.geo.application.query.dto.PagedLegalRegionView;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListLegalRegionsHandlerTest {

	private final CapturingLegalRegionQueryRepository repository = new CapturingLegalRegionQueryRepository();
	private final ListLegalRegionsHandler handler = new ListLegalRegionsHandler(repository);

	@Test
	void listsLegalRegionsWithFilters() {
		repository.result = new LegalRegionPage(List.of(new LegalRegionReadModel(
			"1100000000",
			"서울특별시",
			"서울특별시",
			LegalRegionLevel.SIDO,
			null,
			true
		)), 1);

		PagedLegalRegionView result = handler.handle(new ListLegalRegionsQuery(
			" 서울 ",
			LegalRegionLevel.SIDO,
			null,
			true,
			0,
			20,
			List.of("fullName,asc")
		));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).code()).isEqualTo("1100000000");
		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.totalPages()).isEqualTo(1);
		assertThat(repository.query).isEqualTo("서울");
		assertThat(repository.level).isEqualTo(LegalRegionLevel.SIDO);
		assertThat(repository.isActive).isTrue();
	}

	@Test
	void rejectsInvalidPage() {
		assertThatThrownBy(() -> handler.handle(new ListLegalRegionsQuery(
			null,
			null,
			null,
			null,
			-1,
			20,
			List.of()
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsInvalidParentCode() {
		assertThatThrownBy(() -> handler.handle(new ListLegalRegionsQuery(
			null,
			null,
			"1100",
			null,
			0,
			20,
			List.of()
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	private static class CapturingLegalRegionQueryRepository implements LegalRegionQueryRepository {

		private String query;
		private LegalRegionLevel level;
		private Boolean isActive;
		private LegalRegionPage result = new LegalRegionPage(List.of(), 0);

		@Override
		public LegalRegionPage findLegalRegions(
			String query,
			LegalRegionLevel level,
			String parentCode,
			Boolean isActive,
			int page,
			int size
		) {
			this.query = query;
			this.level = level;
			this.isActive = isActive;
			return result;
		}
	}
}
