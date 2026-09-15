package com.soomgil.geo.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.port.LegalRegionReadModel;
import com.soomgil.geo.application.query.dto.FindLegalRegionsByCodesQuery;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.global.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindLegalRegionsByCodesHandlerTest {
	private final LegalRegionQueryRepository repository = mock(LegalRegionQueryRepository.class);
	private final FindLegalRegionsByCodesHandler handler = new FindLegalRegionsByCodesHandler(repository);

	@Test
	@DisplayName("요청한 코드의 지역을 이름과 함께 돌려준다")
	void returnsViewsForRequestedCodes() {
		when(repository.findLegalRegionsByCodes(List.of("5011000000"))).thenReturn(List.of(
			new LegalRegionReadModel("5011000000", "제주시", "제주특별자치도 제주시", LegalRegionLevel.SIGUNGU, "5000000000", true)
		));

		List<LegalRegionView> result = handler.handle(new FindLegalRegionsByCodesQuery(List.of("5011000000")));

		assertThat(result).extracting(LegalRegionView::name).containsExactly("제주시");
		assertThat(result.getFirst().level()).isEqualTo(LegalRegionLevel.SIGUNGU);
	}

	@Test
	@DisplayName("코드가 없으면 저장소를 호출하지 않고 빈 목록을 준다")
	void returnsEmptyWithoutRepositoryCallWhenCodesEmpty() {
		assertThat(handler.handle(new FindLegalRegionsByCodesQuery(List.of()))).isEmpty();
		verify(repository, never()).findLegalRegionsByCodes(any());
	}

	@Test
	@DisplayName("10자리가 아닌 코드는 거절한다")
	void rejectsMalformedCode() {
		assertThatThrownBy(() -> handler.handle(new FindLegalRegionsByCodesQuery(List.of("123"))))
			.isInstanceOf(BusinessException.class);
	}
}
