package com.soomgil.itinerary.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mapbox API 설정.
 */
@Component
@ConfigurationProperties(prefix = "soomgil.mapbox")
public class MapboxProperties {

	private String baseUrl = "https://api.mapbox.com";
	private String accessToken = "";

	/**
	 * Mapbox API base URL.
	 *
	 * @return base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Mapbox access token.
	 *
	 * @return access token
	 */
	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
}
