package com.soomgil.preference.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PreferencePolicyPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class)
		.withPropertyValues(
			"soomgil.preference.tag-selection.minimum-confidence=0.60",
			"soomgil.preference.recommendation.matched-member-threshold=0.20",
			"soomgil.preference.synthetic-persona.required-count=40",
			"soomgil.preference.real-user.minimum-total-reaction-count=20000"
		);

	@Test
	void bindsRecommendationPolicyValuesFromConfiguration() {
		contextRunner.run(context -> {
			PreferencePolicyProperties properties = context.getBean(PreferencePolicyProperties.class);

			assertThat(properties.getTagSelection().getMinimumConfidence())
				.isEqualByComparingTo(new BigDecimal("0.60"));
			assertThat(properties.getRecommendation().getMatchedMemberThreshold())
				.isEqualByComparingTo(new BigDecimal("0.20"));
			assertThat(properties.getSyntheticPersona().getRequiredCount()).isEqualTo(40);
			assertThat(properties.getRealUser().getMinimumTotalReactionCount()).isEqualTo(20_000);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PreferencePolicyProperties.class)
	static class TestConfiguration {
	}
}
