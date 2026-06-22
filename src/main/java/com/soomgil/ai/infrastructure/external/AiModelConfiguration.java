package com.soomgil.ai.infrastructure.external;

import com.soomgil.ai.application.AiGuideModel;
import com.soomgil.ai.application.AiTripToolsFactory;
import com.soomgil.ai.application.LocalFallbackAiGuideModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AiModelConfiguration {

	private static final Logger log = LoggerFactory.getLogger(AiModelConfiguration.class);

	@Bean
	AiGuideModel aiGuideModel(
		ObjectProvider<ChatModel> chatModels,
		AiTripToolsFactory toolsFactory,
		ObjectMapper objectMapper,
		Environment environment
	) {
		ChatModel chatModel = chatModels.getIfAvailable();
		String modelChat = environment.getProperty("spring.ai.model.chat");
		String apiKeyConfigured = environment.getProperty("spring.ai.google.genai.api-key");
		String apiKeySource = apiKeyConfigured == null || apiKeyConfigured.isBlank()
			? "MISSING/BLANK"
			: "(present, length=" + apiKeyConfigured.length() + ")";

		log.info(
			"AI guide init → spring.ai.model.chat='{}', spring.ai.google.genai.api-key={}, ChatModel bean={}",
			modelChat, apiKeySource, chatModel == null ? "NOT CREATED" : chatModel.getClass().getSimpleName()
		);

		if (chatModel != null) {
			return new SpringAiGuideModel(chatModel, toolsFactory, objectMapper);
		}
		log.warn(
			"ChatModel bean not available. Falling back to LocalFallbackAiGuideModel. "
				+ "To enable Gemini: set spring.ai.model.chat=google-genai and provide a non-empty GEMINI_API_KEY."
		);
		return new LocalFallbackAiGuideModel(toolsFactory, objectMapper);
	}
}
