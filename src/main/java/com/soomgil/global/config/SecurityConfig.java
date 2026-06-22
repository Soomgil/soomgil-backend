package com.soomgil.global.config;

import com.soomgil.auth.application.service.OAuthProperties;
import com.soomgil.global.security.JwtToCurrentUserAuthenticationConverter;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * API 보안 filter chain과 CORS 정책을 구성한다.
 *
 * <p>backend는 stateless API를 기준으로 form login과 HTTP basic을 비활성화한다.
 * 인증 실패와 권한 실패는 각각 ProblemDetails 401/403 응답으로 변환한다.
 */
@Configuration
@EnableConfigurationProperties({CorsProperties.class, OAuthProperties.class})
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		CorsConfigurationSource corsConfigurationSource,
		ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
		ProblemDetailsAccessDeniedHandler accessDeniedHandler,
		JwtToCurrentUserAuthenticationConverter jwtAuthenticationConverter
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/email-verifications").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/email-verification-requests").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/password-reset-requests").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/password-resets").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/oauth/*/authorization-url").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/oauth/*/callback").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/policy-documents").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/community/posts", "/api/v1/stories").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/community/posts/*", "/api/v1/stories/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/community/posts/*/comments", "/api/v1/stories/*/comments").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/community/reports/reasons").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/users/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/users/*/followers", "/api/v1/users/*/following").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/media/files/*/content").permitAll()
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
				.jwtAuthenticationConverter(jwtAuthenticationConverter)
			))
			.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOriginList());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(
			HttpHeaders.AUTHORIZATION,
			HttpHeaders.CONTENT_TYPE,
			HttpHeaders.ACCEPT,
			"X-Request-Id",
			"Idempotency-Key",
			"X-Soomgil-WebSocket-Session-Id"
		));
		configuration.setExposedHeaders(List.of("X-Request-Id"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
