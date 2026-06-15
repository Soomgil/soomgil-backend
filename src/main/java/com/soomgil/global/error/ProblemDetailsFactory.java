package com.soomgil.global.error;

import com.soomgil.common.api.dto.ProblemDetails;
import com.soomgil.common.api.dto.ProblemField;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsFactory {

	private static final String PROBLEM_TYPE_BASE_URI = "https://api.soomgil.example.com/problems/";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	public ProblemDetails create(
		ErrorCode errorCode,
		String detail,
		HttpServletRequest request,
		List<ProblemField> fields
	) {
		String requestId = request.getHeader(REQUEST_ID_HEADER);

		return new ProblemDetails(
			URI.create(PROBLEM_TYPE_BASE_URI + errorCode.code().toLowerCase()),
			errorCode.defaultMessage(),
			errorCode.status().value(),
			detail,
			request.getRequestURI(),
			errorCode.code(),
			requestId,
			fields
		);
	}
}
