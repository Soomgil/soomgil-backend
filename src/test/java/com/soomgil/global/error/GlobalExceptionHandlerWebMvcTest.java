package com.soomgil.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerWebMvcTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ValidationTestController())
			.setControllerAdvice(new GlobalExceptionHandler(new ProblemDetailsFactory()))
			.build();
	}

	@Test
	void returnsProblemDetailsForRequestBodyValidationFailure() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title": ""}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.method").value("POST"))
			.andExpect(jsonPath("$.instance").value("/test/validation"))
			.andExpect(jsonPath("$.fields[0].name").value("title"));
	}

	@RestController
	private static class ValidationTestController {

		@PostMapping("/test/validation")
		TestResponse validate(@Valid @RequestBody ValidationTestRequest request) {
			return new TestResponse(request.title());
		}
	}

	private record ValidationTestRequest(
		@NotBlank
		String title
	) {
	}

	private record TestResponse(
		String title
	) {
	}
}
