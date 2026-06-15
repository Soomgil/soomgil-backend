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

/**
 * Spring Security 인가 실패를 403 ProblemDetails 응답으로 변환한다.
 *
 * <p>사용자는 인증되었지만 resource 접근 권한이 없을 때 사용된다.
 * 인증 자체가 없는 경우는 {@link ProblemDetailsAuthenticationEntryPoint}가 401로 처리한다.
 */
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
