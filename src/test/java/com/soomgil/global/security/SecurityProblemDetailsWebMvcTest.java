package com.soomgil.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soomgil.global.error.ProblemDetailsFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityProblemDetailsWebMvcTest.ProtectedTestController.class)
@Import({
	SecurityProblemDetailsWebMvcTest.TestSecurityConfig.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class,
	ProblemDetailsFactory.class
})
class SecurityProblemDetailsWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsProblemDetailsForUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/test/protected"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.method").value("GET"))
			.andExpect(jsonPath("$.instance").value("/test/protected"))
			.andExpect(jsonPath("$.fields").isEmpty());
	}

	@Test
	@WithMockUser(roles = "USER")
	void returnsProblemDetailsForForbiddenRequest() throws Exception {
		mockMvc.perform(get("/test/admin-only"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.method").value("GET"))
			.andExpect(jsonPath("$.instance").value("/test/admin-only"))
			.andExpect(jsonPath("$.fields").isEmpty());
	}

	@RestController
	static class ProtectedTestController {

		@GetMapping("/test/protected")
		String protectedEndpoint() {
			return "ok";
		}

		@GetMapping("/test/admin-only")
		String adminOnlyEndpoint() {
			return "ok";
		}
	}

	@TestConfiguration
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(
			HttpSecurity http,
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
					.requestMatchers("/test/admin-only").hasRole("ADMIN")
					.anyRequest().authenticated()
				)
				.build();
		}
	}
}
