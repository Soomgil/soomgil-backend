package com.soomgil.ai.infrastructure.external;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "soomgil.ai.gms", name = "enabled", havingValue = "true")
public class GmsGeminiClientConfiguration {

	@Bean
	Client gmsGoogleGenAiClient(Environment environment) {
		String apiKey = environment.getRequiredProperty("spring.ai.google.genai.api-key");
		String baseUrl = environment.getRequiredProperty("soomgil.ai.gms.base-url");
		String apiVersion = environment.getRequiredProperty("soomgil.ai.gms.api-version");

		return Client.builder()
			.apiKey(apiKey.trim())
			.httpOptions(HttpOptions.builder()
				.baseUrl(baseUrl)
				.apiVersion(apiVersion)
				.build())
			.build();
	}
}
