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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ProblemDetailsFactory problemDetailsFactory;
	private final ObjectMapper objectMapper;

	public ProblemDetailsAuthenticationEntryPoint(
		ProblemDetailsFactory problemDetailsFactory,
		ObjectMapper objectMapper
	) {
		this.problemDetailsFactory = problemDetailsFactory;
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		ProblemDetails problemDetails = problemDetailsFactory.create(
			ErrorCode.UNAUTHORIZED,
			ErrorCode.UNAUTHORIZED.defaultMessage(),
			request,
			List.of()
		);

		response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problemDetails);
	}
}
