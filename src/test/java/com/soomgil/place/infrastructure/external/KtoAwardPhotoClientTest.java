package com.soomgil.place.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class KtoAwardPhotoClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void selectsOnlyAnAllowedAwardPhotoThatClearlyMatchesThePlace() throws Exception {
		var response = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
			  {
			    "koTitle":"경복궁의 겨울", "koFilmst":"서울특별시 종로구 경복궁",
			    "koKeyWord":"경복궁, 겨울", "orgImage":"https://img.example/award-type3.jpg",
			    "cpyrhtDivCd":"Type3"
			  },
			  {
			    "koTitle":"경복궁 야경", "koFilmst":"서울특별시 종로구 경복궁",
			    "koKeyWord":"경복궁, 야경", "orgImage":"https://img.example/award-type4.jpg",
			    "cpyrhtDivCd":"Type4"
			  },
			  {
			    "koTitle":"창덕궁의 봄", "koFilmst":"서울특별시 종로구 창덕궁",
			    "koKeyWord":"창덕궁, 봄", "orgImage":"https://img.example/other-place.jpg",
			    "cpyrhtDivCd":"Type1"
			  }
			]}}}}
			""");

		assertThat(KtoAwardPhotoClient.findBestAwardPhoto(response, "경복궁"))
			.contains("https://img.example/award-type3.jpg");
		assertThat(KtoAwardPhotoClient.findBestAwardPhoto(response, "덕수궁"))
			.isEmpty();
	}
}
