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

	@Test
	void parsesEveryCatalogEntryThatHasAnOriginalImage() throws Exception {
		var response = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
			  {
			    "contentId":"DVvwaI", "koTitle":"가야산 설경",
			    "koFilmst":"경상남도 합천군, 가야산국립공원", "koCmanNm":"서정철",
			    "koWnprzDiz":"스마트폰 부문 [입선]", "filmDay":"202401",
			    "orgImage":"https://img.example/org.jpg", "thumbImage":"https://img.example/thumb.jpg",
			    "cpyrhtDivCd":"Type1", "lDongRegnCd":"48", "koKeyWord":"가야산, 설경"
			  },
			  {
			    "contentId":"NoImage", "koTitle":"이미지 없음",
			    "koFilmst":"서울특별시 종로구, 경복궁", "cpyrhtDivCd":"Type1"
			  }
			]}}}}
			""");

		var catalog = KtoAwardPhotoClient.parseCatalog(response);

		assertThat(catalog).hasSize(1);
		assertThat(catalog.getFirst().awardContentId()).isEqualTo("DVvwaI");
		assertThat(catalog.getFirst().filmLocation()).isEqualTo("경상남도 합천군, 가야산국립공원");
		assertThat(catalog.getFirst().photographer()).isEqualTo("서정철");
		assertThat(catalog.getFirst().thumbnailUrl()).isEqualTo("https://img.example/thumb.jpg");
		assertThat(catalog.getFirst().regionCode()).isEqualTo("48");
	}
}
