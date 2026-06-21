package com.soomgil.place.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class KtoTourismPlaceClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void convertsKtoListAndDetailResponsesIntoFrontendPlaceData() throws Exception {
		var listBody = objectMapper.readTree("""
			{
			  "response": {"header": {"resultCode": "0000"}, "body": {"items": {"item": [{
			    "contentid": "126508", "title": "해운대해수욕장", "addr1": "부산 해운대구",
			    "mapx": "129.1604", "mapy": "35.1587", "contenttypeid": "12",
			    "firstimage": "https://img.example/main.jpg", "firstimage2": "https://img.example/sub.jpg",
			    "modifiedtime": "20260619173654"
			  }]}}}
			}
			""");
		var detailBody = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{
			  "contentid":"126508", "overview":"<b>넓은 백사장이 있는 해수욕장</b>"
			}]}}}}
			""");

		var places = KtoTourismPlaceClient.parseList(listBody);
		var enriched = KtoTourismPlaceClient.withDetail(places.getFirst(), detailBody);

		assertThat(enriched.externalPlaceId()).isEqualTo("126508");
		assertThat(enriched.name()).isEqualTo("해운대해수욕장");
		assertThat(enriched.description()).isEqualTo("넓은 백사장이 있는 해수욕장");
		assertThat(enriched.photos()).containsExactly(
			"https://img.example/main.jpg",
			"https://img.example/sub.jpg"
		);
		assertThat(enriched.category()).isEqualTo("관광지");
	}

	@Test
	void rejectsFailedKtoResponseInsteadOfReturningAnEmptyFeed() throws Exception {
		var body = objectMapper.readTree("""
			{"response":{"header":{"resultCode":"30","resultMsg":"SERVICE KEY IS NOT REGISTERED"}}}
			""");

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> KtoTourismPlaceClient.parseList(body))
			.isInstanceOf(KtoTourismPlaceException.class)
			.hasMessageContaining("SERVICE KEY");
	}
}
