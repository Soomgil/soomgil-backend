package com.soomgil.place.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "soomgil.kto")
public class KtoTourismPlaceProperties {

	private String baseUrl = "https://apis.data.go.kr/B551011/KorService2";
	private String awardBaseUrl = "https://apis.data.go.kr/B551011/PhokoAwrdService";
	private String apiKey = "";

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getAwardBaseUrl() {
		return awardBaseUrl;
	}

	public void setAwardBaseUrl(String awardBaseUrl) {
		this.awardBaseUrl = awardBaseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
}
