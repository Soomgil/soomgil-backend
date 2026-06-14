package com.soomgil.global.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "soomgil.cors")
public record CorsProperties(String allowedOrigins) {

	public List<String> allowedOriginList() {
		if (allowedOrigins == null || allowedOrigins.isBlank()) {
			return List.of("http://localhost:5173");
		}

		return Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isBlank())
			.toList();
	}
}
