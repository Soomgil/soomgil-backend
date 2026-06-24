package com.soomgil.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigWebMvcTest.AdminTestController.class)
@Import({
	SecurityConfig.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class,
	ProblemDetailsFactory.class,
	SecurityConfigWebMvcTest.AdminTestController.class
})
class SecurityConfigWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@org.springframework.boot.test.mock.mockito.MockBean
	private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

	@org.springframework.boot.test.mock.mockito.MockBean
	private com.soomgil.global.security.JwtToCurrentUserAuthenticationConverter jwtAuthenticationConverter;

	@Test
	@WithMockUser(roles = "USER")
	void rejectsAdminEndpointForRegularUser() throws Exception {
		mockMvc.perform(get("/api/v1/admin/test"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void allowsAdminEndpointForAdminUser() throws Exception {
		mockMvc.perform(get("/api/v1/admin/test"))
			.andExpect(status().isOk());
	}

	@Test
	void allowsPublicFollowListsWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/users/10000000-0000-0000-0000-000000000001/followers"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/users/10000000-0000-0000-0000-000000000001/following"))
			.andExpect(status().isOk());
	}

	@Test
	void allowsWebSocketHandshakePathWithoutHttpAuthentication() throws Exception {
		mockMvc.perform(get("/ws"))
			.andExpect(status().isOk());
	}

	@RestController
	static class AdminTestController {

		@GetMapping("/api/v1/admin/test")
		String adminOnly() {
			return "ok";
		}

		@GetMapping({"/api/v1/users/{userId}/followers", "/api/v1/users/{userId}/following"})
		String publicFollowLists() {
			return "ok";
		}

		@GetMapping("/ws")
		String websocketHandshakePath() {
			return "ok";
		}
	}
}
