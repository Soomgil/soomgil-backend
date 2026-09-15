package com.soomgil.place.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LegalRegionKtoAreaPolicyTest {
	@Test
	@DisplayName("법정동 시도 코드를 한국관광공사 areaCode로 바꾼다")
	void mapsLegalSidoPrefixToKtoAreaCode() {
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("5011000000")).contains("39");
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("1168000000")).contains("1");
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("2635000000")).contains("6");
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("3000000000")).contains("3");
	}

	@Test
	@DisplayName("특별자치도 신설 코드도 같은 areaCode로 이어진다")
	void mapsRenamedProvincesToTheSameAreaCode() {
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("4200000000")).contains("32");
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("5100000000")).contains("32");
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("4500000000")).contains("37");
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("5200000000")).contains("37");
	}

	@Test
	@DisplayName("모르는 시도나 형식이 다른 코드는 비운다")
	void returnsEmptyForUnknownOrMalformedCodes() {
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("9900000000")).isEmpty();
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf("39")).isEmpty();
		assertThat(LegalRegionKtoAreaPolicy.areaCodeOf(null)).isEmpty();
	}
}
