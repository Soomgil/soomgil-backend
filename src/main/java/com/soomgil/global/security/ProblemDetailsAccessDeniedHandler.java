package com.soomgil.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.ProblemDetails;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.error.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

	private final ProblemDetailsFactory problemDetailsFactory;
	private final ObjectMapper objectMapper;

	public ProblemDetailsAccessDeniedHandler(
		ProblemDetailsFactory problemDetailsFactory,
		ObjectMapper objectMapper
	) {
		this.problemDetailsFactory = problemDetailsFactory;
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException {
		ProblemDetails problemDetails = problemDetailsFactory.create(
			ErrorCode.FORBIDDEN,
			ErrorCode.FORBIDDEN.defaultMessage(),
			request,
			List.of()
		);

		response.setStatus(ErrorCode.FORBIDDEN.status().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problemDetails);
	}
}
