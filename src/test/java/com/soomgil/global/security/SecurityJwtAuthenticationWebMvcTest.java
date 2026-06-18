package com.soomgil.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.soomgil.global.web.HealthCheckController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWT access token 검증 파이프라인을 {@code MockMvc}로 종단 검증한다.
 *
 * <p>검증 범위:
 * <ul>
 *   <li>유효한 JWT → {@code @AuthenticationPrincipal CurrentUser} 주입 + 200 응답</li>
 *   <li>Authorization 헤더 없음 → 401 ProblemDetails</li>
 *   <li>만료된 JWT → 401</li>
 *   <li>잘못된 서명 JWT → 401</li>
 *   <li>health endpoint는 인증 없이 200</li>
 * </ul>
 *
 * <p>{@link JwtConfiguration}의 HS256 {@code JwtDecoder}/{@code JwtEncoder} 빈과
 * {@link JwtToCurrentUserAuthenticationConverter}를 실제 main 컴포넌트로 사용한다.
 * 테스트 전용 {@code SecurityFilterChain}에서 {@code oauth2ResourceServer().jwt()}를 연결한다.
 */
@WebMvcTest(controllers = {SecurityJwtAuthenticationWebMvcTest.ProtectedTestController.class, HealthCheckController.class})
@Import({
	SecurityJwtAuthenticationWebMvcTest.TestSecurityConfig.class,
	SecurityJwtAuthenticationWebMvcTest.ProtectedTestController.class,
	JwtConfiguration.class,
	JwtToCurrentUserAuthenticationConverter.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class,
	com.soomgil.global.error.ProblemDetailsFactory.class
})
@TestPropertySource(properties = {
	"soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz",
	"soomgil.security.jwt.access-token-ttl-seconds=900",
	"soomgil.security.jwt.issuer=soomgil"
})
class SecurityJwtAuthenticationWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	@DisplayName("유효한 JWT로 보호된 endpoint를 호출하면 200과 현재 사용자 정보를 반환한다")
	void validJwtReturns200WithCurrentUser() throws Exception {
		UUID userId = UUID.randomUUID();
		String token = mintJwt(userId, "user@example.com", Instant.now().plusSeconds(900));

		mockMvc.perform(get("/test/jwt-protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(userId.toString()))
			.andExpect(jsonPath("$.email").value("user@example.com"));
	}

	@Test
	@DisplayName("email claim이 없는 JWT도 허용된다")
	void validJwtWithoutEmailClaimIsAccepted() throws Exception {
		UUID userId = UUID.randomUUID();
		String token = mintJwt(userId, null, Instant.now().plusSeconds(900));

		mockMvc.perform(get("/test/jwt-protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(userId.toString()))
			.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	@DisplayName("Authorization 헤더가 없으면 401 ProblemDetails를 반환한다")
	void missingAuthorizationHeaderReturns401() throws Exception {
		mockMvc.perform(get("/test/jwt-protected"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	@DisplayName("Bearer prefix가 없으면 401을 반환한다")
	void missingBearerPrefixReturns401() throws Exception {
		UUID userId = UUID.randomUUID();
		String token = mintJwt(userId, "user@example.com", Instant.now().plusSeconds(900));

		mockMvc.perform(get("/test/jwt-protected")
				.header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("만료된 JWT는 401을 반환한다")
	void expiredJwtReturns401() throws Exception {
		UUID userId = UUID.randomUUID();
		Instant issuedAt = Instant.now().minusSeconds(3600);
		String token = mintJwt(userId, "user@example.com", issuedAt, issuedAt.plusSeconds(60));

		mockMvc.perform(get("/test/jwt-protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("잘못된 서명의 JWT는 401을 반환한다")
	void invalidSignatureJwtReturns401() throws Exception {
		mockMvc.perform(get("/test/jwt-protected")
				.header(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.signature"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("health endpoint는 인증 없이 200을 반환한다")
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
			.andExpect(status().isOk());
	}

	private String mintJwt(UUID userId, String email, Instant expiresAt) {
		return mintJwt(userId, email, Instant.now(), expiresAt);
	}

	private String mintJwt(UUID userId, String email, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer("soomgil")
			.subject(userId.toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt);
		if (email != null) {
			claims.claim("email", email);
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

	@RestController
	static class ProtectedTestController {

		@GetMapping("/test/jwt-protected")
		java.util.Map<String, Object> protectedEndpoint(@AuthenticationPrincipal CurrentUser currentUser) {
			java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
			body.put("userId", currentUser.userId());
			if (currentUser.email() != null) {
				body.put("email", currentUser.email());
			}
			return body;
		}
	}

	@TestConfiguration
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(
			HttpSecurity http,
			JwtToCurrentUserAuthenticationConverter converter,
			ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
			ProblemDetailsAccessDeniedHandler accessDeniedHandler
		) throws Exception {
			return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(exception -> exception
					.authenticationEntryPoint(authenticationEntryPoint)
					.accessDeniedHandler(accessDeniedHandler)
				)
				.authorizeHttpRequests(auth -> auth
					.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/health").permitAll()
					.requestMatchers("/test/jwt-protected").authenticated()
					.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
					.jwtAuthenticationConverter(converter)
				))
				.build();
		}
	}
}
