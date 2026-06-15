package com.soomgil.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.common.api.dto.ProblemDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

	@Test
	void convertsBusinessExceptionToProblemDetails() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler(new ProblemDetailsFactory());
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/trips");
		request.addHeader("X-Request-Id", "req-123");

		ResponseEntity<ProblemDetails> response = handler.handleBusinessException(
			new BusinessException(ErrorCode.FORBIDDEN, "Only trip members can edit this trip."),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(ErrorCode.FORBIDDEN.status());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
		assertThat(response.getBody().status()).isEqualTo(403);
		assertThat(response.getBody().detail()).isEqualTo("Only trip members can edit this trip.");
		assertThat(response.getBody().instance()).isEqualTo("/api/v1/trips");
		assertThat(response.getBody().requestId()).isEqualTo("req-123");
		assertThat(response.getBody().fields()).isEmpty();
	}
}
