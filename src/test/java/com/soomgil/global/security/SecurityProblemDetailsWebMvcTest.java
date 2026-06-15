package com.soomgil.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soomgil.global.config.SecurityConfig;
import com.soomgil.global.error.ProblemDetailsFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityProblemDetailsWebMvcTest.ProtectedTestController.class)
@Import({ SecurityConfig.class, ProblemDetailsAuthenticationEntryPoint.class, ProblemDetailsFactory.class })
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

	@RestController
	static class ProtectedTestController {

		@GetMapping("/test/protected")
		String protectedEndpoint() {
			return "ok";
		}
	}
}
