package com.soomgil.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.Client;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GmsGeminiClientConfigurationTest {

	@Test
	void configuresGoogleClientToUseGmsGeminiGateway() throws Exception {
		GmsGeminiClientConfiguration configuration = new GmsGeminiClientConfiguration();

		MockEnvironment environment = new MockEnvironment()
			.withProperty("spring.ai.google.genai.api-key", "gms-test-key")
			.withProperty("soomgil.ai.gms.base-url", "https://gms.example/gmsapi/generativelanguage.googleapis.com")
			.withProperty("soomgil.ai.gms.api-version", "v1beta");

		try (Client client = configuration.gmsGoogleGenAiClient(environment)) {
			Method baseUrl = Client.class.getDeclaredMethod("baseUrl");
			baseUrl.setAccessible(true);
			Optional<?> configuredBaseUrl = (Optional<?>) baseUrl.invoke(client);

			assertThat(client.apiKey()).isEqualTo("gms-test-key");
			assertThat(configuredBaseUrl.orElseThrow())
				.isEqualTo("https://gms.example/gmsapi/generativelanguage.googleapis.com");
		}
	}
}
