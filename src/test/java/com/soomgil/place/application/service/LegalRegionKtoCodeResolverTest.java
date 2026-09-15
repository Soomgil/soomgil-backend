package com.soomgil.place.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.application.query.handler.FindLegalRegionsByCodesHandler;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.place.application.port.KtoRegionCode;
import com.soomgil.place.application.port.TourismSourceRegionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegalRegionKtoCodeResolverTest {
	private final FindLegalRegionsByCodesHandler legalRegions = mock(FindLegalRegionsByCodesHandler.class);
	private final TourismSourceRegionRepository regions = mock(TourismSourceRegionRepository.class);
	private final LegalRegionKtoCodeResolver resolver = new LegalRegionKtoCodeResolver(legalRegions, regions);

	@Test
	@DisplayName("시도 코드는 areaCode만 만들고 시군구 조회를 하지 않는다")
	void sidoLevelCodeMapsToAreaCodeOnly() {
		assertThat(resolver.resolve(List.of("5000000000"))).containsExactly(new KtoRegionCode("39", null));
		verify(regions, never()).findGugunCode(anyInt(), anyString());
	}

	@Test
	@DisplayName("시군구 코드는 법정동 이름으로 관광공사 시군구 코드를 찾는다")
	void sigunguLevelCodeUsesGugunNameMatch() {
		when(legalRegions.handle(any())).thenReturn(List.of(view("5011000000", "제주시", LegalRegionLevel.SIGUNGU)));
		when(regions.findGugunCode(39, "제주시")).thenReturn(Optional.of(4));

		assertThat(resolver.resolve(List.of("5011000000"))).containsExactly(new KtoRegionCode("39", "4"));
	}

	@Test
	@DisplayName("시군구 이름을 못 찾으면 시도 범위로 넓힌다")
	void unmatchedSigunguDegradesToSido() {
		when(legalRegions.handle(any())).thenReturn(List.of(view("5011000000", "제주시", LegalRegionLevel.SIGUNGU)));
		when(regions.findGugunCode(39, "제주시")).thenReturn(Optional.empty());

		assertThat(resolver.resolve(List.of("5011000000"))).containsExactly(new KtoRegionCode("39", null));
	}

	@Test
	@DisplayName("모르는 시도는 건너뛰고 같은 결과는 한 번만 남긴다")
	void skipsUnknownSidoAndCollapsesDuplicates() {
		assertThat(resolver.resolve(List.of("9911000000", "5000000000", "5000000000")))
			.containsExactly(new KtoRegionCode("39", null));
	}

	private static LegalRegionView view(String code, String name, LegalRegionLevel level) {
		return new LegalRegionView(code, name, "제주특별자치도 " + name, level, "5000000000", true);
	}
}
