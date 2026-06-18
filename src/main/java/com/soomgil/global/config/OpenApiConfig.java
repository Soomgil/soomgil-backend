package com.soomgil.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 문서 구성.
 *
 * <p>JWT Bearer 인증 스킴을 등록해 Swagger UI 우측 상단의 "Authorize" 버튼에서
 * access token을 한 번만 입력하면 모든 인증 필요 엔드포인트에 자동 적용되도록 한다.
 */
@Configuration
public class OpenApiConfig {

	/**
	 * OpenAPI 메타 정보와 JWT Bearer 보안 스킴을 정의한다.
	 *
	 * @return 구성된 OpenAPI 빈
	 */
	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Soomgil API")
				.description("숨길 공동 여행 기획 플랫폼 백엔드 API")
				.version("v1"))
			.components(new Components().addSecuritySchemes(
				"bearerAuth",
				new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.in(SecurityScheme.In.HEADER)
					.name("Authorization")
			));
	}
}
