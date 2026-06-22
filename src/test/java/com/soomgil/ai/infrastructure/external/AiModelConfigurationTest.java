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
	"spring.ai.model.chat=google-genai",
	"spring.ai.google.genai.api-key=test-key"
})
class AiModelConfigurationTest {

	@Autowired
	private AiGuideModel model;

	@Test
	void activatesSpringAiModelForGoogleGenAiProviderCode() {
		assertThat(model).isInstanceOf(SpringAiGuideModel.class);
	}
}
