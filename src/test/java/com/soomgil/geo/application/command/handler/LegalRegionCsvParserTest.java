package com.soomgil.geo.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.geo.application.port.LegalRegionUpsert;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegalRegionCsvParserTest {

	private static final Instant SYNCED_AT = Instant.parse("2026-06-17T00:00:00Z");

	@Test
	void parsesGovernmentLegalRegionTsv() {
		String content = """
			법정동코드	법정동명	폐지여부
			1100000000	서울특별시	존재
			1111000000	서울특별시 종로구	존재
			1111010100	서울특별시 종로구 청운동	존재
			""";

		List<LegalRegionUpsert> regions = LegalRegionCsvParser.parse(content, SYNCED_AT);

		assertThat(regions).hasSize(3);
		assertThat(regions.get(0)).satisfies(region -> {
			assertThat(region.code()).isEqualTo("1100000000");
			assertThat(region.name()).isEqualTo("서울특별시");
			assertThat(region.fullName()).isEqualTo("서울특별시");
			assertThat(region.level()).isEqualTo(LegalRegionLevel.SIDO);
			assertThat(region.parentCode()).isNull();
			assertThat(region.sidoCode()).isEqualTo("11");
			assertThat(region.sigunguCode()).isNull();
			assertThat(region.eupmyeondongCode()).isNull();
			assertThat(region.active()).isTrue();
		});
		assertThat(regions.get(1)).satisfies(region -> {
			assertThat(region.name()).isEqualTo("종로구");
			assertThat(region.level()).isEqualTo(LegalRegionLevel.SIGUNGU);
			assertThat(region.parentCode()).isEqualTo("1100000000");
			assertThat(region.sigunguCode()).isEqualTo("11110");
		});
		assertThat(regions.get(2)).satisfies(region -> {
			assertThat(region.name()).isEqualTo("청운동");
			assertThat(region.level()).isEqualTo(LegalRegionLevel.EUPMYEONDONG);
			assertThat(region.parentCode()).isEqualTo("1111000000");
			assertThat(region.eupmyeondongCode()).isEqualTo("11110101");
		});
	}

	@Test
	void marksRemovedRowsInactive() {
		String content = """
			법정동코드	법정동명	폐지여부
			1111010100	서울특별시 종로구 청운동	폐지
			""";

		List<LegalRegionUpsert> regions = LegalRegionCsvParser.parse(content, SYNCED_AT);

		assertThat(regions.get(0).active()).isFalse();
		assertThat(regions.get(0).rawStatus()).isEqualTo("폐지");
	}

	@Test
	void rejectsInvalidCode() {
		String content = """
			법정동코드	법정동명	폐지여부
			11110	서울특별시 종로구	존재
			""";

		assertThatThrownBy(() -> LegalRegionCsvParser.parse(content, SYNCED_AT))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
			);
	}
}
