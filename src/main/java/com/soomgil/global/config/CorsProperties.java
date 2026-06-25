package com.soomgil.global.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * frontend origin 허용 목록과 패턴을 담는 CORS 설정 property.
 *
 * <p>{@code soomgil.cors.allowed-origins}를 쉼표로 구분해 설정한다.
 * 임시 배포 URL처럼 origin이 고정되지 않는 환경은
 * {@code soomgil.cors.allowed-origin-patterns}를 쉼표로 구분해 설정한다.
 * 값이 없으면 로컬 Vite 개발 서버인 {@code http://localhost:5173}을 기본값으로 사용한다.
 */
@ConfigurationProperties(prefix = "soomgil.cors")
public record CorsProperties(String allowedOrigins, String allowedOriginPatterns) {

	/**
	 * 쉼표로 구분된 origin 문자열을 Spring CORS 설정에 사용할 목록으로 변환한다.
	 *
	 * @return 허용 origin 목록
	 */
	public List<String> allowedOriginList() {
		if (allowedOrigins == null || allowedOrigins.isBlank()) {
			return List.of("http://localhost:5173");
		}

		return Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isBlank())
			.toList();
	}

	/**
	 * 쉼표로 구분된 origin pattern 문자열을 Spring CORS 설정에 사용할 목록으로 변환한다.
	 *
	 * @return 허용 origin pattern 목록
	 */
	public List<String> allowedOriginPatternList() {
		if (allowedOriginPatterns == null || allowedOriginPatterns.isBlank()) {
			return List.of();
		}

		return Arrays.stream(allowedOriginPatterns.split(","))
			.map(String::trim)
			.filter(pattern -> !pattern.isBlank())
			.toList();
	}
}
