package com.soomgil.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.TestcontainersConfiguration;
import com.soomgil.ai.application.AiGuideModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
	"spring.ai.model.chat=openai",
	"spring.ai.openai.api-key=test-key",
	"spring.ai.openai.chat.options.model=gpt-5.6-terra",
	"soomgil.ai.gms.enabled=false"
})
class OpenAiModelConfigurationTest {

	@Autowired
	private AiGuideModel model;

	@Test
	void activatesSpringAiModelForOpenAiProviderCode() {
		assertThat(model).isInstanceOf(SpringAiGuideModel.class);
	}
}
