package com.soomgil.global.error;

import com.soomgil.common.api.dto.ProblemDetails;
import com.soomgil.common.api.dto.ProblemField;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * error code와 servlet request 정보를 API 공통 ProblemDetails body로 변환한다.
 *
 * <p>{@code instance}는 요청 URI, {@code method}는 HTTP method, {@code requestId}는
 * {@code X-Request-Id} header에서 가져온다.
 */
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
			request.getMethod(),
			errorCode.code(),
			requestId,
			fields
		);
	}
}
