package com.soomgil.ai.infrastructure.external;

import com.soomgil.ai.application.AiGuideModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

	@Bean
	@ConditionalOnMissingBean(ChatModel.class)
	AiGuideModel unavailableAiGuideModel() {
		return new UnavailableAiGuideModel();
	}
}
