package com.soomgil.ai.infrastructure.external;

import com.soomgil.ai.application.AiGuideModel;
import com.soomgil.ai.application.AiTripToolsFactory;
import com.soomgil.ai.application.LocalFallbackAiGuideModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

	@Bean
	AiGuideModel aiGuideModel(ObjectProvider<ChatModel> chatModels,
		AiTripToolsFactory toolsFactory,
		ObjectMapper objectMapper) {
		ChatModel chatModel = chatModels.getIfAvailable();
		if (chatModel != null) {
			return new SpringAiGuideModel(chatModel, toolsFactory, objectMapper);
		}
		return new LocalFallbackAiGuideModel(toolsFactory, objectMapper);
	}
}
